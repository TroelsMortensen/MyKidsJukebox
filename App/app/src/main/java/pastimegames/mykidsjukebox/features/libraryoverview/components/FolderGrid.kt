package pastimegames.mykidsjukebox.features.libraryoverview.components

import android.content.Context
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
    context: Context,
    scanner: LibraryScanner,
    quickScanResult: LibraryScanner.QuickScanResult?,
    playEnabled: Boolean,
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
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = items,
            key = { item -> item.targetUri.toString() }
        ) { item ->
            FolderGridCard(
                item = item,
                context = context,
                scanner = scanner,
                quickScanResult = quickScanResult,
                playEnabled = playEnabled,
                onClick = { onItemClick(item) },
                onPlayClick = { onPlayClick(item) }
            )
        }
    }
}
