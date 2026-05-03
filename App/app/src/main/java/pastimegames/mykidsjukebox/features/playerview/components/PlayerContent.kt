package pastimegames.mykidsjukebox.features.playerview.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val layoutTokens = remember(maxWidth, maxHeight) {
            buildPlayerLayoutTokens(
                maxWidth = maxWidth,
                maxHeight = maxHeight
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(layoutTokens.contentPadding),
            verticalArrangement = Arrangement.spacedBy(layoutTokens.sectionSpacing)
        ) {
            LargeCloseButton(
                onClick = onBack,
                height = layoutTokens.closeButtonHeight,
                iconSize = layoutTokens.closeIconSize,
                cornerRadius = layoutTokens.closeCornerRadius
            )

            PlayerTitle(title = state.title)

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center
            ) {
                val artworkSize = minOf(maxHeight, maxWidth)

                PlayerArtwork(
                    artworkUri = state.artworkUri,
                    modifier = Modifier.size(artworkSize)
                )
            }

            UpcomingQueueRow(
                items = state.upcomingItems,
                artworkByAudioUri = state.artworkByAudioUri,
                onQueueItemClick = onQueueItemClick,
                layoutTokens = layoutTokens
            )

            PlayerPlaybackControls(
                isPlaying = state.isPlaying,
                positionMs = state.positionMs,
                remainingMs = state.remainingMs,
                progress = state.progress,
                layoutTokens = layoutTokens,
                onPlayPauseClick = {
                    uiFeedback.performPrimaryActionFeedback()
                    onPlayPauseClick()
                }
            )
        }
    }
}
