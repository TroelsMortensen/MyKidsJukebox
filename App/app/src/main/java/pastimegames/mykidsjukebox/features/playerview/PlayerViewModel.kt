package pastimegames.mykidsjukebox.features.playerview

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerViewModel(
    application: Application,
    title: String,
    artworkUri: Uri?,
    audioUri: Uri
) : AndroidViewModel(application) {

    private val exoPlayer = ExoPlayer.Builder(application).build()
    private val _state = MutableStateFlow(
        PlayerState(
            title = title,
            artworkUri = artworkUri,
            isPlaying = false,
            durationMs = 0L,
            positionMs = 0L
        )
    )
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var progressJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { current -> current.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            refreshProgress()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            refreshProgress()
        }
    }

    init {
        exoPlayer.addListener(playerListener)
        exoPlayer.setMediaItem(MediaItem.fromUri(audioUri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        progressJob = viewModelScope.launch {
            while (true) {
                refreshProgress()
                delay(250)
            }
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun stopPlayback() {
        exoPlayer.stop()
    }

    private fun refreshProgress() {
        val duration = exoPlayer.duration.takeIf { it > 0L } ?: 0L
        _state.update { current ->
            current.copy(
                durationMs = duration,
                positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                isPlaying = exoPlayer.isPlaying
            )
        }
    }

    override fun onCleared() {
        progressJob?.cancel()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
        super.onCleared()
    }

    companion object {
        fun factory(
            application: Application,
            title: String,
            artworkUri: Uri?,
            audioUri: Uri
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlayerViewModel(
                        application = application,
                        title = title,
                        artworkUri = artworkUri,
                        audioUri = audioUri
                    ) as T
                }
            }
        }
    }
}
