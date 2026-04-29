package pastimegames.mykidsjukebox.features.libraryoverview.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import pastimegames.mykidsjukebox.data.library.FolderGridItem
import pastimegames.mykidsjukebox.data.library.LibraryScanner

@Composable
fun FolderGrid(
    items: List<FolderGridItem>,
    scanner: LibraryScanner,
    quickScanResult: LibraryScanner.QuickScanResult?,
    onItemClick: (FolderGridItem) -> Unit,
    onPlayClick: (FolderGridItem) -> Unit
) {
    val configuration = LocalConfiguration.current
    val columnCount = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        3
    } else {
        2
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = items,
            key = { item -> item.targetUri.toString() }
        ) { item ->
            FolderGridCard(
                item = item,
                scanner = scanner,
                quickScanResult = quickScanResult,
                onClick = { onItemClick(item) },
                onPlayClick = { onPlayClick(item) }
            )
        }
    }
}
