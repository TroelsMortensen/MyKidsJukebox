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
    private val folderMetadataCache = mutableMapOf<Uri, FolderMetadata>()

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

        data class ItemsUpdated(
            val items: List<FolderGridItem>,
            val quickScanResult: QuickScanResult
        ) : ScanEvent

        data class Complete(
            val hasBrowsableContent: Boolean,
            val quickScanResult: QuickScanResult,
            val sortedAudioItems: List<FolderGridItem>
        ) : ScanEvent

        data object Failed : ScanEvent
    }

    fun scanFolderItemsIncremental(
        context: Context,
        currentFolder: DocumentFile,
        batchSize: Int = 20
    ): Flow<ScanEvent> = channelFlow {
        folderMetadataCache.clear()
        val childDirectoryUris = linkedSetOf<Uri>()
        val siblingJpgByLowerName = mutableMapOf<String, Uri>()
        val pendingFolderItems = mutableListOf<FolderGridItem>()
        val pendingAudioShellItems = mutableListOf<FolderGridItem>()
        val audioShellItems = mutableListOf<FolderGridItem>()
        var hasBrowsableContent = false
        var hasEmittedAny = false

        fun snapshotQuickScan(): QuickScanResult {
            return QuickScanResult(
                childDirectoryUris = childDirectoryUris.toSet(),
                siblingJpgByLowerName = siblingJpgByLowerName.toMap()
            )
        }

        suspend fun emitBatch(pendingItems: MutableList<FolderGridItem>, force: Boolean) {
            if (pendingItems.isEmpty()) {
                return
            }
            val shouldFlush = force || !hasEmittedAny || pendingItems.size >= batchSize
            if (!shouldFlush) {
                return
            }
            send(
                ScanEvent.Batch(
                    items = pendingItems.toList(),
                    quickScanResult = snapshotQuickScan()
                )
            )
            pendingItems.clear()
            hasEmittedAny = true
        }

        val listedSuccessfully = forEachChildDocument(context, currentFolder.uri) { child ->
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
                emitBatch(pendingFolderItems, force = false)
                return@forEachChildDocument
            }

            val childName = child.displayName ?: return@forEachChildDocument
            if (childName.hasJpgExtension()) {
                siblingJpgByLowerName[childName.lowercase()] = child.documentUri
                return@forEachChildDocument
            }
            if (childName.hasAudioExtension(AUDIO_EXTENSION_MP3)) {
                hasBrowsableContent = true
                val shellItem = FolderGridItem(
                    name = childName.substringBeforeLast('.', missingDelimiterValue = childName),
                    targetUri = child.documentUri,
                    artworkUri = null,
                    artworkIsLoading = true,
                    kind = LibraryItemKind.Audio
                )
                audioShellItems += shellItem
                pendingAudioShellItems += shellItem
                emitBatch(pendingAudioShellItems, force = false)
            }
        }

        emitBatch(pendingFolderItems, force = true)
        emitBatch(pendingAudioShellItems, force = true)

        if (!listedSuccessfully) {
            send(ScanEvent.Failed)
            return@channelFlow
        }

        val audioUpdates = buildAudioArtworkUpdates(audioShellItems, siblingJpgByLowerName)
        if (audioUpdates.isNotEmpty()) {
            audioUpdates.chunked(batchSize).forEach { batch ->
                send(
                    ScanEvent.ItemsUpdated(
                        items = batch,
                        quickScanResult = snapshotQuickScan()
                    )
                )
            }
        }

        val sortedAudioItems = audioUpdates.sortedBy { it.name.lowercase() }
        send(
            ScanEvent.Complete(
                hasBrowsableContent = hasBrowsableContent,
                quickScanResult = snapshotQuickScan(),
                sortedAudioItems = sortedAudioItems
            )
        )
    }.flowOn(Dispatchers.IO)

    suspend fun resolveArtworkForItem(
        context: Context,
        item: FolderGridItem,
        quickScanResult: QuickScanResult
    ): Uri? {
        return when (item.kind) {
            LibraryItemKind.Folder -> {
                if (!quickScanResult.childDirectoryUris.contains(item.targetUri)) {
                    return null
                }
                resolveFolderMetadata(context, item.targetUri)?.coverUri
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

    suspend fun resolveFolderCounts(
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
        val metadata = resolveFolderMetadata(context, item.targetUri) ?: return null
        return FolderCounts(
            childFolderCount = metadata.childFolderCount,
            audioFileCount = metadata.audioFileCount
        )
    }

    private suspend fun resolveFolderMetadata(context: Context, folderUri: Uri): FolderMetadata? {
        folderMetadataCache[folderUri]?.let { return it }
        var coverUri: Uri? = null
        var childFolderCount = 0
        var audioFileCount = 0
        queryChildDocuments(context, folderUri).forEach { child ->
            if (child.isDirectory) {
                childFolderCount += 1
                return@forEach
            }
            val name = child.displayName ?: return@forEach
            if (name.equals(FOLDER_ARTWORK_FILE_NAME, ignoreCase = true)) {
                coverUri = child.documentUri
            }
            if (name.hasAudioExtension(AUDIO_EXTENSION_MP3)) {
                audioFileCount += 1
            }
        }
        return FolderMetadata(
            coverUri = coverUri,
            childFolderCount = childFolderCount,
            audioFileCount = audioFileCount
        ).also { folderMetadataCache[folderUri] = it }
    }
}

private data class FolderMetadata(
    val coverUri: Uri?,
    val childFolderCount: Int,
    val audioFileCount: Int
)

internal fun buildAudioArtworkUpdates(
    audioShellItems: List<FolderGridItem>,
    siblingJpgByLowerName: Map<String, Uri>
): List<FolderGridItem> {
    return audioShellItems.map { shellItem ->
        val expectedArtwork = "${shellItem.name}.$ARTWORK_EXTENSION_JPG"
        shellItem.copy(
            artworkUri = siblingJpgByLowerName[expectedArtwork.lowercase()],
            artworkIsLoading = false
        )
    }
}

internal fun String.hasJpgExtension(): Boolean {
    return substringAfterLast('.', missingDelimiterValue = "").equals(ARTWORK_EXTENSION_JPG, ignoreCase = true)
}

private fun String.hasAudioExtension(expectedExtension: String): Boolean {
    return substringAfterLast('.', missingDelimiterValue = "").equals(expectedExtension, ignoreCase = true)
}
