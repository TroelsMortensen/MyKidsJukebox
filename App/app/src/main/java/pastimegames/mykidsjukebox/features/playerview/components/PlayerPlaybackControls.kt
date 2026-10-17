package pastimegames.mykidsjukebox.features.playerview.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SwitchAccessShortcut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import pastimegames.mykidsjukebox.R

private val RestartButtonBackground = Color(0xFF219BF3)

@Composable
internal fun PlayerPlaybackControls(
    isPlaying: Boolean,
    positionMs: Long,
    remainingMs: Long,
    progress: Float,
    layoutTokens: PlayerLayoutTokens,
    onPlayPauseClick: () -> Unit,
    onRestartClick: () -> Unit,
    onSkipBackClick: () -> Unit,
    onSkipForwardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatMillis(positionMs),
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false
        )
        Spacer(Modifier.width(layoutTokens.timelineItemSpacing))
        SkipButton(
            onClick = onSkipBackClick,
            imageVector = Icons.Filled.SwitchAccessShortcut,
            contentDescription = stringResource(R.string.skip_back_10),
            buttonSize = layoutTokens.skipButtonSize,
            iconSize = layoutTokens.skipIconSize,
            // Rotate 90° clockwise, then mirror horizontally.
            iconModifier = Modifier
                .scale(scaleX = -1f, scaleY = 1f)
                .rotate(90f)
        )
        Spacer(Modifier.width(layoutTokens.skipButtonSpacing))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(layoutTokens.timelineHeight),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.width(layoutTokens.skipButtonSpacing))
        SkipButton(
            onClick = onSkipForwardClick,
            imageVector = Icons.Filled.SwitchAccessShortcut,
            contentDescription = stringResource(R.string.skip_forward_10),
            buttonSize = layoutTokens.skipButtonSize,
            iconSize = layoutTokens.skipIconSize,
            iconModifier = Modifier.rotate(90f)
        )
        Spacer(Modifier.width(layoutTokens.timelineItemSpacing))
        Text(
            text = "-${formatMillis(remainingMs)}",
            textAlign = TextAlign.Start,
            maxLines = 1,
            softWrap = false
        )
    }

    Spacer(modifier = Modifier.height(layoutTokens.controlsSpacing))

    val playButtonScale by animateFloatAsState(
        targetValue = if (isPlaying) 0.98f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "play-button-scale"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onRestartClick,
            modifier = Modifier.size(layoutTokens.restartButtonSize),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = RestartButtonBackground,
                contentColor = Color.White
            ),
            border = null
        ) {
            Icon(
                imageVector = Icons.Filled.Replay,
                contentDescription = stringResource(R.string.restart),
                modifier = Modifier.size(layoutTokens.restartIconSize)
            )
        }

        Spacer(Modifier.width(layoutTokens.playbackControlsSpacing))

        Button(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .weight(1f)
                .height(layoutTokens.playButtonHeight)
                .scale(playButtonScale),
            shape = RoundedCornerShape(percent = 50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) {
                    stringResource(R.string.pause)
                } else {
                    stringResource(R.string.play)
                },
                modifier = Modifier.size(layoutTokens.playIconSize)
            )
        }
    }
}

@Composable
private fun SkipButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
    buttonSize: Dp,
    iconSize: Dp,
    iconModifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(buttonSize),
        shape = CircleShape,
        contentPadding = PaddingValues(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ),
        border = null
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(iconSize)
                .then(iconModifier)
        )
    }
}
