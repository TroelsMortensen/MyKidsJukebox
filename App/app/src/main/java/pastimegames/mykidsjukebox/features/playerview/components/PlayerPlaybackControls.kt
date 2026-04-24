package pastimegames.mykidsjukebox.features.playerview.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pastimegames.mykidsjukebox.R

@Composable
internal fun PlayerPlaybackControls(
    isPlaying: Boolean,
    positionMs: Long,
    remainingMs: Long,
    progress: Float,
    layoutTokens: PlayerLayoutTokens,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatMillis(positionMs),
            textAlign = TextAlign.End,
            modifier = Modifier.width(layoutTokens.timelineTextWidth)
        )
        Spacer(Modifier.width(layoutTokens.timelineItemSpacing))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(layoutTokens.timelineHeight),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.width(layoutTokens.timelineItemSpacing))
        Text(
            text = "-${formatMillis(remainingMs)}",
            textAlign = TextAlign.Start,
            modifier = Modifier.width(layoutTokens.timelineTextWidth)
        )
    }

    Spacer(modifier = Modifier.height(layoutTokens.controlsSpacing))

    val playButtonScale by animateFloatAsState(
        targetValue = if (isPlaying) 0.98f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "play-button-scale"
    )

    Button(
        onClick = onPlayPauseClick,
        modifier = Modifier
            .fillMaxWidth()
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
