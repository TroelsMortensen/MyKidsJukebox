package pastimegames.mykidsjukebox.data.library

import android.net.Uri
import androidx.documentfile.provider.DocumentFile

private val audioExtensions = setOf("mp3", "m4a", "wav")

class LibraryScanner {
    data class QuickScanResult(
        val items: List<FolderGridItem>,
        val currentFolderChildren: Array<DocumentFile>,
        val childDirectoriesByUri: Map<Uri, DocumentFile>
    )

    data class FolderCounts(
        val childFolderCount: Int,
        val audioFileCount: Int
    )

    fun listFolderItemsQuick(currentFolder: DocumentFile): QuickScanResult {
        val children = currentFolder.listFiles()
        val folderItems = children
            .filter { it.isDirectory }
            .map { folder ->
                FolderGridItem(
                    name = folder.name ?: "Unknown",
                    targetUri = folder.uri,
                    artworkUri = null,
                    artworkIsLoading = true,
                    kind = LibraryItemKind.Folder,
                    childFolderCount = null,
                    audioFileCount = null
                )
            }

        val audioItems = children
            .filter { it.isAudioFile() }
            .map { audio ->
                FolderGridItem(
                    name = audio.displayNameWithoutExtension(),
                    targetUri = audio.uri,
                    artworkUri = null,
                    artworkIsLoading = true,
                    kind = LibraryItemKind.Audio
                )
            }

        return QuickScanResult(
            items = (folderItems + audioItems).sortedBy { it.name.lowercase() },
            currentFolderChildren = children,
            childDirectoriesByUri = children
                .filter { it.isDirectory }
                .associateBy { it.uri }
        )
    }

    fun resolveArtworkForItem(item: FolderGridItem, quickScanResult: QuickScanResult): Uri? {
        return when (item.kind) {
            LibraryItemKind.Folder -> {
                val directory = quickScanResult.childDirectoriesByUri[item.targetUri] ?: return null
                findFolderArtworkUri(directory.listFiles())
            }

            LibraryItemKind.Audio -> {
                val audioDocument = quickScanResult.currentFolderChildren
                    .firstOrNull { child -> child.uri == item.targetUri && child.isFile }
                    ?: return null
                findAudioArtworkUri(audioDocument, quickScanResult.currentFolderChildren)
            }
        }
    }

    fun resolveFolderCounts(item: FolderGridItem, quickScanResult: QuickScanResult): FolderCounts? {
        if (item.kind != LibraryItemKind.Folder) {
            return null
        }
        val directory = quickScanResult.childDirectoriesByUri[item.targetUri] ?: return null
        val folderChildren = directory.listFiles()
        return FolderCounts(
            childFolderCount = folderChildren.count { it.isDirectory },
            audioFileCount = folderChildren.count { it.isAudioFile() }
        )
    }

    fun hasAnyBrowsableContent(folder: DocumentFile): Boolean {
        return folder.listFiles().any { it.isDirectory || it.isAudioFile() }
    }

    private fun findFolderArtworkUri(children: Array<DocumentFile>): Uri? {
        val cover = children.firstOrNull { it.isFile && it.name.equals("cover.jpg", ignoreCase = true) }
        if (cover != null) {
            return cover.uri
        }

        val artwork = children.firstOrNull { it.isFile && it.name.equals("artwork.jpg", ignoreCase = true) }
        return artwork?.uri
    }

    private fun findAudioArtworkUri(audioFile: DocumentFile, siblings: Array<DocumentFile>): Uri? {
        val audioName = audioFile.name ?: return null
        val baseName = audioName.substringBeforeLast('.', missingDelimiterValue = audioName)
        return siblings.firstOrNull { sibling ->
            if (!sibling.isFile) {
                return@firstOrNull false
            }
            val siblingName = sibling.name ?: return@firstOrNull false
            siblingName.equals("$baseName.jpg", ignoreCase = true)
        }?.uri
    }
}

private fun DocumentFile.isAudioFile(): Boolean {
    if (!isFile) {
        return false
    }

    val extension = name
        ?.substringAfterLast('.', missingDelimiterValue = "")
        ?.lowercase()
        ?: return false
    return extension in audioExtensions
}

private fun DocumentFile.displayNameWithoutExtension(): String {
    val fileName = name ?: return "Unknown Audio"
    return fileName.substringBeforeLast('.', missingDelimiterValue = fileName)
}
