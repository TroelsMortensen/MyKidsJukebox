package pastimegames.mykidsjukebox.features.libraryoverview

import pastimegames.mykidsjukebox.data.library.FolderGridItem

data class LibraryOverviewState(
    val isRootSelected: Boolean,
    val showBackButton: Boolean,
    val currentFolderName: String,
    val folderItems: List<FolderGridItem>,
    val hasBrowsableContent: Boolean,
    val errorMessage: String?
)
