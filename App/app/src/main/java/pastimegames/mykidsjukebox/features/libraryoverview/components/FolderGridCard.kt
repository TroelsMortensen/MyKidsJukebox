package pastimegames.mykidsjukebox.features.libraryoverview.components

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pastimegames.mykidsjukebox.data.library.FolderGridItem
import pastimegames.mykidsjukebox.data.library.LibraryItemKind
import pastimegames.mykidsjukebox.data.library.LibraryScanner
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FolderGridCard(
    item: FolderGridItem,
    context: Context,
    scanner: LibraryScanner,
    quickScanResult: LibraryScanner.QuickScanResult?,
    playEnabled: Boolean,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    var artworkUri by remember(item.targetUri) { mutableStateOf<Uri?>(item.artworkUri) }
    var artworkIsLoading by remember(item.targetUri) { mutableStateOf(item.artworkIsLoading) }
    var childFolderCount by remember(item.targetUri) { mutableStateOf(item.childFolderCount) }
    var audioFileCount by remember(item.targetUri) { mutableStateOf(item.audioFileCount) }

    LaunchedEffect(item.targetUri, quickScanResult) {
        val quickScan = quickScanResult ?: run {
            artworkIsLoading = false
            return@LaunchedEffect
        }
        val resolvedArtworkUri = withContext(Dispatchers.IO) {
            scanner.resolveArtworkForItem(context, item, quickScan)
        }
        val resolvedCounts = withContext(Dispatchers.IO) {
            scanner.resolveFolderCounts(context, item, quickScan)
        }
        artworkUri = resolvedArtworkUri ?: artworkUri
        artworkIsLoading = false
        childFolderCount = resolvedCounts?.childFolderCount
        audioFileCount = resolvedCounts?.audioFileCount
    }

    val resolvedItem = item.copy(
        artworkUri = artworkUri,
        artworkIsLoading = artworkIsLoading,
        childFolderCount = childFolderCount,
        audioFileCount = audioFileCount
    )

    val colorScheme = MaterialTheme.colorScheme
    val cardBackgroundColor = if (resolvedItem.kind == LibraryItemKind.Audio) {
        colorScheme.surfaceVariant
    } else {
        colorScheme.surface
    }

    val shouldShowPlayButton = resolvedItem.kind == LibraryItemKind.Audio
    val cardAspectRatio = if (resolvedItem.kind == LibraryItemKind.Audio) 0.68f else 0.78f
    val maxCardWidth = 320.dp

    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxCardWidth)
                .aspectRatio(cardAspectRatio)
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
                    text = resolvedItem.name,
                    textAlign = TextAlign.Center,
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                    contentAlignment = Alignment.TopCenter
                ) {
                    FolderArtwork(
                        artworkUri = resolvedItem.artworkUri,
                        artworkIsLoading = resolvedItem.artworkIsLoading,
                        itemKind = resolvedItem.kind,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                    )
                }
                if (shouldShowPlayButton) {
                    Button(
                        onClick = onPlayClick,
                        enabled = playEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(30.dp),
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
}
