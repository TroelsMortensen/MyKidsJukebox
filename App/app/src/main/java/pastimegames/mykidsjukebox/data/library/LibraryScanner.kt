package pastimegames.mykidsjukebox.data.library

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import pastimegames.mykidsjukebox.storage.forEachChildDocument
import pastimegames.mykidsjukebox.storage.queryChildDocuments

private const val FOLDER_ARTWORK_FILE_NAME = "cover.jpg"
private const val AUDIO_EXTENSION_MP3 = "mp3"
private const val ARTWORK_EXTENSION_JPG = "jpg"

class LibraryScanner {
    data class QuickScanResult(
        val childDirectoryUris: Set<Uri>,
        val siblingJpgByLowerName: Map<String, Uri>
    )

    data class FolderCounts(
        val childFolderCount: Int,
        val audioFileCount: Int
    )

    sealed interface ScanEvent {
        data class Batch(
            val items: List<FolderGridItem>,
            val quickScanResult: QuickScanResult
        ) : ScanEvent

        data class Complete(
            val hasBrowsableContent: Boolean,
            val quickScanResult: QuickScanResult,
            val sortedAudioItems: List<FolderGridItem>
        ) : ScanEvent
    }

    fun scanFolderItemsIncremental(
        context: Context,
        currentFolder: DocumentFile,
        batchSize: Int = 20
    ): Flow<ScanEvent> = channelFlow {
        val childDirectoryUris = linkedSetOf<Uri>()
        val siblingJpgByLowerName = mutableMapOf<String, Uri>()
        val pendingAudioEntries = mutableListOf<AudioEntry>()
        val emittedAudioItems = mutableListOf<FolderGridItem>()
        val pendingFolderItems = mutableListOf<FolderGridItem>()
        var hasBrowsableContent = false

        forEachChildDocument(context, currentFolder.uri) { child ->
            if (child.isDirectory) {
                hasBrowsableContent = true
                childDirectoryUris += child.documentUri
                pendingFolderItems += FolderGridItem(
                    name = child.displayName ?: "Unknown",
                    targetUri = child.documentUri,
                    artworkUri = null,
                    artworkIsLoading = true,
                    kind = LibraryItemKind.Folder,
                    childFolderCount = null,
                    audioFileCount = null
                )

                if (pendingFolderItems.size >= batchSize) {
                    trySend(
                        ScanEvent.Batch(
                            items = pendingFolderItems.toList(),
                            quickScanResult = QuickScanResult(
                                childDirectoryUris = childDirectoryUris.toSet(),
                                siblingJpgByLowerName = siblingJpgByLowerName.toMap()
                            )
                        )
                    )
                    pendingFolderItems.clear()
                }
                return@forEachChildDocument
            }

            val childName = child.displayName ?: return@forEachChildDocument
            if (childName.hasJpgExtension()) {
                siblingJpgByLowerName[childName.lowercase()] = child.documentUri
                return@forEachChildDocument
            }
            if (childName.hasAudioExtension(AUDIO_EXTENSION_MP3)) {
                hasBrowsableContent = true
                pendingAudioEntries += AudioEntry(name = childName, uri = child.documentUri)
            }
        }

        if (pendingFolderItems.isNotEmpty()) {
            trySend(
                ScanEvent.Batch(
                    items = pendingFolderItems.toList(),
                    quickScanResult = QuickScanResult(
                        childDirectoryUris = childDirectoryUris.toSet(),
                        siblingJpgByLowerName = siblingJpgByLowerName.toMap()
                    )
                )
            )
            pendingFolderItems.clear()
        }

        val pendingAudioItems = pendingAudioEntries.map { audioEntry ->
            val audioName = audioEntry.name
            val expectedArtwork = "${audioName.substringBeforeLast('.', missingDelimiterValue = audioName)}.$ARTWORK_EXTENSION_JPG"
            FolderGridItem(
                name = audioName.substringBeforeLast('.', missingDelimiterValue = audioName),
                targetUri = audioEntry.uri,
                artworkUri = siblingJpgByLowerName[expectedArtwork.lowercase()],
                artworkIsLoading = false,
                kind = LibraryItemKind.Audio
            )
        }

        pendingAudioItems.chunked(batchSize).forEach { batch ->
            emittedAudioItems += batch
            trySend(
                ScanEvent.Batch(
                    items = batch,
                    quickScanResult = QuickScanResult(
                        childDirectoryUris = childDirectoryUris.toSet(),
                        siblingJpgByLowerName = siblingJpgByLowerName.toMap()
                    )
                )
            )
        }

        val sortedAudioItems = emittedAudioItems.sortedBy { it.name.lowercase() }
        trySend(
            ScanEvent.Complete(
                hasBrowsableContent = hasBrowsableContent,
                quickScanResult = QuickScanResult(
                    childDirectoryUris = childDirectoryUris.toSet(),
                    siblingJpgByLowerName = siblingJpgByLowerName.toMap()
                ),
                sortedAudioItems = sortedAudioItems
            )
        )
    }.flowOn(Dispatchers.IO)

    fun resolveArtworkForItem(
        context: Context,
        item: FolderGridItem,
        quickScanResult: QuickScanResult
    ): Uri? {
        return when (item.kind) {
            LibraryItemKind.Folder -> {
                if (!quickScanResult.childDirectoryUris.contains(item.targetUri)) {
                    return null
                }
                queryChildDocuments(context, item.targetUri).firstOrNull { child ->
                    if (!child.isDirectory && child.displayName.equals(FOLDER_ARTWORK_FILE_NAME, ignoreCase = true)) {
                        return@firstOrNull true
                    }
                    false
                }?.documentUri
            }

            LibraryItemKind.Audio -> {
                if (item.artworkUri != null) {
                    item.artworkUri
                } else {
                    val audioName = "${item.name}.$AUDIO_EXTENSION_MP3"
                    val expectedArtwork = "${audioName.substringBeforeLast('.', missingDelimiterValue = audioName)}.$ARTWORK_EXTENSION_JPG"
                    quickScanResult.siblingJpgByLowerName[expectedArtwork.lowercase()]
                }
            }
        }
    }

    fun resolveFolderCounts(
        context: Context,
        item: FolderGridItem,
        quickScanResult: QuickScanResult
    ): FolderCounts? {
        if (item.kind != LibraryItemKind.Folder) {
            return null
        }
        if (!quickScanResult.childDirectoryUris.contains(item.targetUri)) {
            return null
        }
        var childFolderCount = 0
        var audioFileCount = 0
        queryChildDocuments(context, item.targetUri).forEach { child ->
            if (child.isDirectory) {
                childFolderCount += 1
                return@forEach
            }
            if (child.displayName?.hasAudioExtension(AUDIO_EXTENSION_MP3) == true) {
                audioFileCount += 1
            }
        }
        return FolderCounts(
            childFolderCount = childFolderCount,
            audioFileCount = audioFileCount
        )
    }
}

private data class AudioEntry(val name: String, val uri: Uri)

internal data class ScanEntry(
    val name: String?,
    val uri: Uri,
    val isDirectory: Boolean,
    val isMp3Audio: Boolean,
    val isJpg: Boolean,
    val folderDetails: FolderDetails? = null
)

internal data class FolderDetails(
    val coverArtworkUri: Uri?,
    val childFolderCount: Int,
    val audioFileCount: Int
)

internal fun buildItemsFromEntries(entries: List<ScanEntry>): List<FolderGridItem> {
    val siblingJpgByLowerName = entries
        .asSequence()
        .filter { !it.isDirectory && it.isJpg }
        .mapNotNull { entry ->
            val name = entry.name ?: return@mapNotNull null
            name.lowercase() to entry.uri
        }
        .toMap()

    val folderItems = entries
        .asSequence()
        .filter { it.isDirectory }
        .map { entry ->
            val details = entry.folderDetails
            FolderGridItem(
                name = entry.name ?: "Unknown",
                targetUri = entry.uri,
                artworkUri = details?.coverArtworkUri,
                artworkIsLoading = true,
                kind = LibraryItemKind.Folder,
                childFolderCount = details?.childFolderCount,
                audioFileCount = details?.audioFileCount
            )
        }

    val audioItems = entries
        .asSequence()
        .filter { !it.isDirectory && it.isMp3Audio }
        .map { entry ->
            val audioName = entry.name ?: "Unknown Audio"
            val jpgName = "${audioName.substringBeforeLast('.', missingDelimiterValue = audioName)}.$ARTWORK_EXTENSION_JPG"
            FolderGridItem(
                name = audioName.substringBeforeLast('.', missingDelimiterValue = audioName),
                targetUri = entry.uri,
                artworkUri = siblingJpgByLowerName[jpgName.lowercase()],
                artworkIsLoading = false,
                kind = LibraryItemKind.Audio
            )
        }

    return (folderItems + audioItems).toList().sortedBy { it.name.lowercase() }
}

private fun DocumentFile.isMp3AudioFile(): Boolean {
    if (!isFile) {
        return false
    }

    val extension = name
        ?.substringAfterLast('.', missingDelimiterValue = "")
        ?.lowercase()
        ?: return false
    return extension == AUDIO_EXTENSION_MP3
}

internal fun String.hasJpgExtension(): Boolean {
    return substringAfterLast('.', missingDelimiterValue = "").equals(ARTWORK_EXTENSION_JPG, ignoreCase = true)
}

private fun String.hasAudioExtension(expectedExtension: String): Boolean {
    return substringAfterLast('.', missingDelimiterValue = "").equals(expectedExtension, ignoreCase = true)
}
