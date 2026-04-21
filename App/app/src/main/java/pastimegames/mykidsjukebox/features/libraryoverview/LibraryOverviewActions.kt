package pastimegames.mykidsjukebox.features.libraryoverview

import pastimegames.mykidsjukebox.data.library.FolderGridItem

data class LibraryOverviewActions(
    val onSelectFolderClick: () -> Unit,
    val onBackClick: () -> Unit,
    val onItemClick: (FolderGridItem) -> Unit,
    val onPlayClick: (FolderGridItem) -> Unit
)
