package pastimegames.mykidsjukebox.features.playerview

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import pastimegames.mykidsjukebox.features.playerview.components.PlayerContent

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
