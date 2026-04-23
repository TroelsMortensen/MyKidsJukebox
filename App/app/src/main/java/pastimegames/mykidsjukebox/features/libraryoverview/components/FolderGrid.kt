package pastimegames.mykidsjukebox.features.libraryoverview.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pastimegames.mykidsjukebox.data.library.FolderGridItem

@Composable
fun FolderGrid(
    items: List<FolderGridItem>,
    onItemClick: (FolderGridItem) -> Unit,
    onPlayClick: (FolderGridItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(40.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = items,
            key = { item -> item.targetUri.toString() }
        ) { item ->
            FolderGridCard(
                item = item,
                onClick = { onItemClick(item) },
                onPlayClick = { onPlayClick(item) }
            )
        }
    }
}
