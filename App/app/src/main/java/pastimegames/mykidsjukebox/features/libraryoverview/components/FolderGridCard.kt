package pastimegames.mykidsjukebox.features.libraryoverview.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val cardBackgroundColor = when (item.kind) {
        LibraryItemKind.Audio -> Color(0xFF475569)
        LibraryItemKind.Folder -> {
            if ((item.childFolderCount ?: 0) == 0) {
                Color(0xFF334155)
            } else {
                Color(0xFF1E293B)
            }
        }
    }

    val shouldShowPlayButton = when (item.kind) {
        LibraryItemKind.Audio -> true
        LibraryItemKind.Folder -> (item.childFolderCount ?: 0) == 0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(48.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.name,
                textAlign = TextAlign.Center,
                color = Color.White
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
                        containerColor = Color(0xFF22C55E),
                        contentColor = Color(0xFF052E16)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(66.dp)
                    )
                }
            } else {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                )
            }
        }
    }
}
