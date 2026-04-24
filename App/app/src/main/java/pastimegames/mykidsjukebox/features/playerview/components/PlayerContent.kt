package pastimegames.mykidsjukebox.features.playerview.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pastimegames.mykidsjukebox.features.playerview.PlayerQueueItem
import pastimegames.mykidsjukebox.features.playerview.PlayerState
import pastimegames.mykidsjukebox.features.shared.components.LargeCloseButton
import pastimegames.mykidsjukebox.features.shared.components.rememberUiFeedback

@Composable
internal fun PlayerContent(
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

        PlayerTitle(title = state.title)

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

        PlayerPlaybackControls(
            isPlaying = state.isPlaying,
            positionMs = state.positionMs,
            remainingMs = state.remainingMs,
            progress = state.progress,
            onPlayPauseClick = {
                uiFeedback.performPrimaryActionFeedback()
                onPlayPauseClick()
            }
        )
    }
}
