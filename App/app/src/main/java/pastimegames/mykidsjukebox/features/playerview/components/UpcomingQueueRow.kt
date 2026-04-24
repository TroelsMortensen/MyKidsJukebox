package pastimegames.mykidsjukebox.features.playerview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import pastimegames.mykidsjukebox.R
import pastimegames.mykidsjukebox.features.playerview.PlayerQueueItem

@Composable
internal fun UpcomingQueueRow(
    items: List<PlayerQueueItem>,
    onQueueItemClick: (PlayerQueueItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.up_next),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val baseQueueItemSize = maxWidth * 0.20f
            val queueShrinkStep = 6.dp

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(
                    space = 12.dp,
                    alignment = Alignment.CenterHorizontally
                )
            ) {
                itemsIndexed(items) { index, queueItem ->
                    val queueItemSize = baseQueueItemSize - (queueShrinkStep * index)
                    QueueArtworkThumbnail(
                        item = queueItem,
                        itemSize = queueItemSize,
                        onClick = { onQueueItemClick(queueItem) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueArtworkThumbnail(
    item: PlayerQueueItem,
    itemSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (item.artworkUri != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.artworkUri)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.upcoming_artwork_description, item.title),
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(itemSize)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
        )
        return
    }

    Box(
        modifier = modifier
            .size(itemSize)
            .background(
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Audiotrack,
            contentDescription = stringResource(R.string.default_upcoming_artwork_description),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(42.dp)
        )
    }
}
