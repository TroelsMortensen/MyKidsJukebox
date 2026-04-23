package pastimegames.mykidsjukebox.features.playerview

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerViewModel(
    application: Application,
    folderAudioItems: List<PlayerQueueItem>,
    startIndex: Int,
    initialWindowSize: Int
) : AndroidViewModel(application) {
    private val orderedFolderItems = folderAudioItems
    private val safeStartIndex = startIndex.coerceIn(0, (orderedFolderItems.size - 1).coerceAtLeast(0))
    private val queueWindowSize = initialWindowSize.coerceAtLeast(2)
    private val _state = MutableStateFlow(
        PlayerState(
            title = orderedFolderItems.getOrNull(safeStartIndex)?.title ?: "Unknown Audio",
            artworkUri = orderedFolderItems.getOrNull(safeStartIndex)?.artworkUri,
            upcomingItems = orderedFolderItems.drop(safeStartIndex + 1).take(4),
            isPlaying = false,
            durationMs = 0L,
            positionMs = 0L
        )
    )
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var progressJob: Job? = null
    private var mediaController: MediaController? = null
    private var hasSentInitialQueue = false
    private val sessionToken = SessionToken(
        application,
        ComponentName(application, PlaybackService::class.java)
    )
    private val controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()

    private val controllerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { current -> current.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            refreshProgress()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            refreshCurrentTrackMetadata()
            refreshProgress()
        }
    }

    init {
        controllerFuture.addListener({
            val controller = controllerFuture.get()
            mediaController = controller
            controller.addListener(controllerListener)
            sendQueueCommandIfNeeded()
            refreshCurrentTrackMetadata()
            refreshProgress()
        }, ContextCompat.getMainExecutor(application))

        progressJob = viewModelScope.launch {
            while (true) {
                refreshProgress()
                delay(250)
            }
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_IDLE) {
                controller.prepare()
            }
            controller.play()
        }
    }

    fun stopPlaybackForExit() {
        val controller = mediaController ?: return
        controller.pause()
        controller.seekTo(0L)
        controller.playWhenReady = false
        refreshProgress()
    }

    fun playQueueItem(item: PlayerQueueItem) {
        val selectedIndex = orderedFolderItems.indexOfFirst { it.audioUri == item.audioUri }
        if (selectedIndex < 0) {
            return
        }
        sendQueueCommand(startIndex = selectedIndex)
    }

    private fun refreshProgress() {
        val controller = mediaController ?: return
        val duration = controller.duration.takeIf { it > 0L } ?: 0L
        _state.update { current ->
            current.copy(
                durationMs = duration,
                positionMs = controller.currentPosition.coerceAtLeast(0L),
                isPlaying = controller.isPlaying
            )
        }
    }

    private fun sendQueueCommandIfNeeded() {
        if (hasSentInitialQueue || orderedFolderItems.isEmpty()) {
            return
        }
        hasSentInitialQueue = true
        sendQueueCommand(startIndex = safeStartIndex)
    }

    private fun sendQueueCommand(startIndex: Int) {
        val controller = mediaController ?: return
        val payload = QueueCommandPayload(
            items = orderedFolderItems,
            startIndex = startIndex,
            queueWindowSize = queueWindowSize
        )
        val command = SessionCommand(COMMAND_SET_QUEUE, Bundle.EMPTY)
        val commandFuture = controller.sendCustomCommand(command, payload.toBundle())
        commandFuture.addListener({
            val result = commandFuture.get()
            if (result.resultCode == SessionResult.RESULT_SUCCESS) {
                controller.play()
                refreshCurrentTrackMetadata()
                refreshProgress()
            }
        }, ContextCompat.getMainExecutor(getApplication()))
    }

    private fun refreshCurrentTrackMetadata() {
        val mediaId = mediaController?.currentMediaItem?.mediaId
        val folderIndex = mediaId?.toIntOrNull() ?: safeStartIndex
        val currentItem = orderedFolderItems.getOrNull(folderIndex) ?: return

        _state.update { current ->
            current.copy(
                title = currentItem.title,
                artworkUri = currentItem.artworkUri,
                upcomingItems = orderedFolderItems.drop(folderIndex + 1).take(4)
            )
        }
    }

    override fun onCleared() {
        progressJob?.cancel()
        mediaController?.removeListener(controllerListener)
        mediaController?.release()
        mediaController = null
        super.onCleared()
    }

    companion object {
        fun factory(
            application: Application,
            folderAudioItems: List<PlayerQueueItem>,
            startIndex: Int,
            initialWindowSize: Int
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlayerViewModel(
                        application = application,
                        folderAudioItems = folderAudioItems,
                        startIndex = startIndex,
                        initialWindowSize = initialWindowSize
                    ) as T
                }
            }
        }
    }
}
