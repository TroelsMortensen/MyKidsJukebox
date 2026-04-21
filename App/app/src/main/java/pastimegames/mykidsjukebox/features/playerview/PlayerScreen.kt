package pastimegames.mykidsjukebox.features.playerview

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import pastimegames.mykidsjukebox.features.shared.components.LargeCloseButton

@Composable
fun PlayerScreen(
    route: PlayerRoute.Player,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: PlayerViewModel = viewModel(
        key = "player-${route.audioUri}",
        factory = PlayerViewModel.factory(
            application = application,
            title = route.title,
            artworkUri = route.artworkUri,
            audioUri = route.audioUri
        )
    )
    val state by viewModel.state.collectAsState()
    val handleBack = {
        viewModel.stopPlayback()
        onBack()
    }

    BackHandler(onBack = handleBack)

    PlayerContent(
        state = state,
        onBack = handleBack,
        onPlayPauseClick = viewModel::togglePlayPause,
        modifier = modifier
    )
}

@Composable
private fun PlayerContent(
    state: PlayerState,
    onBack: () -> Unit,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        LargeCloseButton(onClick = onBack)

        Text(
            text = state.title,
            fontWeight = FontWeight.Bold
        )

        PlayerArtwork(
            artworkUri = state.artworkUri,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
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
                    .height(10.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "-${formatMillis(state.remainingMs)}",
                textAlign = TextAlign.Start,
                modifier = Modifier.width(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            shape = RoundedCornerShape(percent = 50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF22C55E),
                contentColor = if (state.isPlaying) Color(0xFF052E16) else Color.White
            )
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                modifier = Modifier.size(200.dp)
            )
        }
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
            contentDescription = "Current audio artwork",
            contentScale = ContentScale.Fit,
            modifier = modifier.aspectRatio(1f)
        )
        return
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(color = Color(0xFFFBBF24), shape = RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Audiotrack,
            contentDescription = "Default audio artwork",
            tint = Color(0xFFB91C1C),
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
