package pastimegames.mykidsjukebox.features.playerview

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import pastimegames.mykidsjukebox.R
import pastimegames.mykidsjukebox.features.shared.components.LargeCloseButton
import pastimegames.mykidsjukebox.features.shared.components.rememberUiFeedback

@Composable
fun PlayerScreen(
    route: PlayerRoute.Player,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: PlayerViewModel = viewModel(
        key = "player-${route.startIndex}-${route.sessionId}",
        factory = PlayerViewModel.factory(
            application = application,
            folderAudioItems = route.folderAudioItems,
            startIndex = route.startIndex,
            initialWindowSize = route.initialWindowSize
        )
    )
    val state by viewModel.state.collectAsState()
    val handleBack = {
        viewModel.stopPlaybackForExit()
        onBack()
    }

    BackHandler(onBack = handleBack)

    PlayerContent(
        state = state,
        onBack = handleBack,
        onPlayPauseClick = viewModel::togglePlayPause,
        onQueueItemClick = viewModel::playQueueItem,
        modifier = modifier
    )
}

@Composable
private fun PlayerContent(
    state: PlayerState,
    onBack: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onQueueItemClick: (PlayerQueueItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiFeedback = rememberUiFeedback()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        LargeCloseButton(onClick = onBack)

        Text(
            text = state.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PlayerArtwork(
            artworkUri = state.artworkUri,
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .align(Alignment.CenterHorizontally)
                .weight(0.85f, fill = true)
        )

        UpcomingQueueRow(
            items = state.upcomingItems,
            onQueueItemClick = onQueueItemClick
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatMillis(state.positionMs),
                textAlign = TextAlign.End,
                modifier = Modifier.width(56.dp)
            )
            Spacer(Modifier.width(8.dp))
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "-${formatMillis(state.remainingMs)}",
                textAlign = TextAlign.Start,
                modifier = Modifier.width(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val playButtonScale by animateFloatAsState(
            targetValue = if (state.isPlaying) 0.98f else 1f,
            animationSpec = tween(durationMillis = 180),
            label = "play-button-scale"
        )
        Button(
            onClick = {
                uiFeedback.performPrimaryActionFeedback()
                onPlayPauseClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .scale(playButtonScale),
            shape = RoundedCornerShape(percent = 50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (state.isPlaying) {
                    stringResource(R.string.pause)
                } else {
                    stringResource(R.string.play)
                },
                modifier = Modifier.size(200.dp)
            )
        }
    }
}

@Composable
private fun UpcomingQueueRow(
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

@Composable
private fun PlayerArtwork(
    artworkUri: android.net.Uri?,
    modifier: Modifier = Modifier
) {
    if (artworkUri != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artworkUri)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.current_audio_artwork_description),
            contentScale = ContentScale.Fit,
            modifier = modifier.aspectRatio(1f)
        )
        return
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(18.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Audiotrack,
            contentDescription = stringResource(R.string.default_audio_artwork_description),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(96.dp)
        )
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L) / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
