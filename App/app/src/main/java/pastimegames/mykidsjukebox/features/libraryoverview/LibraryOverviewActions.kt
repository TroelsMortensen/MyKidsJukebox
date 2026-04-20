package pastimegames.mykidsjukebox.features.libraryoverview

import pastimegames.mykidsjukebox.data.library.FolderGridItem

data class LibraryOverviewActions(
    val onSelectFolderClick: () -> Unit,
    val onBackClick: () -> Unit,
    val onParentalSettingsClick: () -> Unit,
    val onFolderClick: (FolderGridItem) -> Unit
)
