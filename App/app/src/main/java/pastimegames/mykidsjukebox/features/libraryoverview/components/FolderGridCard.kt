package pastimegames.mykidsjukebox.features.libraryoverview.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pastimegames.mykidsjukebox.data.library.FolderGridItem
import pastimegames.mykidsjukebox.data.library.LibraryItemKind

@Composable
fun FolderGridCard(
    item: FolderGridItem,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val cardBackgroundColor = if (item.kind == LibraryItemKind.Audio) {
        colorScheme.surfaceVariant
    } else {
        colorScheme.surface
    }

    val shouldShowPlayButton = item.kind == LibraryItemKind.Audio
    val cardHeight = if (item.kind == LibraryItemKind.Audio) 390.dp else 320.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(RoundedCornerShape(36.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(36.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.name,
                textAlign = TextAlign.Center,
                color = colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.TopCenter
            ) {
                val artworkSize = minOf(maxWidth, maxHeight)
                FolderArtwork(
                    artworkUri = item.artworkUri,
                    artworkIsLoading = item.artworkIsLoading,
                    itemKind = item.kind,
                    modifier = Modifier.size(artworkSize)
                )
            }
            if (shouldShowPlayButton) {
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = RoundedCornerShape(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.secondary,
                        contentColor = colorScheme.onSecondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(58.dp)
                    )
                }
            }
        }
    }
}
