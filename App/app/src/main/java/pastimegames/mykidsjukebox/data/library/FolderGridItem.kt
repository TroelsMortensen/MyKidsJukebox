package pastimegames.mykidsjukebox.data.library

import android.net.Uri

enum class LibraryItemKind {
    Folder,
    Audio
}

data class FolderGridItem(
    val name: String,
    val targetUri: Uri,
    val artworkUri: Uri?,
    val kind: LibraryItemKind,
    val childFolderCount: Int? = null,
    val audioFileCount: Int? = null
)
