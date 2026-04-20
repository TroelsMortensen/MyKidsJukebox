package pastimegames.mykidsjukebox.data.library

import android.net.Uri

data class FolderGridItem(
    val name: String,
    val folderUri: Uri,
    val artworkUri: Uri?,
    val childFolderCount: Int,
    val audioFileCount: Int
)
