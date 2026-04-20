package pastimegames.mykidsjukebox.data.library

import android.net.Uri
import androidx.documentfile.provider.DocumentFile

private val audioExtensions = setOf("mp3", "m4a", "wav")

class LibraryScanner {
    fun listFolderItems(currentFolder: DocumentFile): List<FolderGridItem> {
        return currentFolder.listFiles()
            .filter { it.isDirectory }
            .sortedBy { it.name?.lowercase() ?: "" }
            .map { folder ->
                val children = folder.listFiles()
                FolderGridItem(
                    name = folder.name ?: "Unknown",
                    folderUri = folder.uri,
                    artworkUri = findArtworkUri(children),
                    childFolderCount = children.count { it.isDirectory },
                    audioFileCount = children.count { it.isAudioFile() }
                )
            }
    }

    fun hasAnyBrowsableContent(folder: DocumentFile): Boolean {
        return folder.listFiles().any { it.isDirectory || it.isAudioFile() }
    }

    private fun findArtworkUri(children: Array<DocumentFile>): Uri? {
        val cover = children.firstOrNull { it.isFile && it.name.equals("cover.jpg", ignoreCase = true) }
        if (cover != null) {
            return cover.uri
        }

        val artwork = children.firstOrNull { it.isFile && it.name.equals("artwork.jpg", ignoreCase = true) }
        return artwork?.uri
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
