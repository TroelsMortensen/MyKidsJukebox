package pastimegames.mykidsjukebox.data.library

import android.net.Uri
import androidx.documentfile.provider.DocumentFile

private val audioExtensions = setOf("mp3", "m4a", "wav")

class LibraryScanner {
    fun listFolderItems(currentFolder: DocumentFile): List<FolderGridItem> {
        val children = currentFolder.listFiles()
        val folderItems = children
            .filter { it.isDirectory }
            .map { folder ->
                val folderChildren = folder.listFiles()
                FolderGridItem(
                    name = folder.name ?: "Unknown",
                    targetUri = folder.uri,
                    artworkUri = findFolderArtworkUri(folderChildren),
                    kind = LibraryItemKind.Folder,
                    childFolderCount = folderChildren.count { it.isDirectory },
                    audioFileCount = folderChildren.count { it.isAudioFile() }
                )
            }

        val audioItems = children
            .filter { it.isAudioFile() }
            .map { audio ->
                FolderGridItem(
                    name = audio.displayNameWithoutExtension(),
                    targetUri = audio.uri,
                    artworkUri = findAudioArtworkUri(audio, children),
                    kind = LibraryItemKind.Audio
                )
            }

        return (folderItems + audioItems).sortedBy { it.name.lowercase() }
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
