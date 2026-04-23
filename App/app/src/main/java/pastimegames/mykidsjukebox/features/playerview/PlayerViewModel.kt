package pastimegames.mykidsjukebox.features.playerview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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
    folderAudioItems: List<PlayerQueueItem>,
    startIndex: Int,
    initialWindowSize: Int
) : AndroidViewModel(application) {

    private val exoPlayer = ExoPlayer.Builder(application).build()
    private val orderedFolderItems = folderAudioItems
    private val safeStartIndex = startIndex.coerceIn(0, (orderedFolderItems.size - 1).coerceAtLeast(0))
    private val queueWindowSize = initialWindowSize.coerceAtLeast(2)
    private var nextFolderIndexToEnqueue = safeStartIndex
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

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { current -> current.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            ensureQueueDepth()
            refreshProgress()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            trimConsumedQueueItems()
            ensureQueueDepth()
            refreshCurrentTrackMetadata()
            refreshProgress()
        }
    }

    init {
        if (orderedFolderItems.isNotEmpty()) {
            exoPlayer.addListener(playerListener)
            val initialMediaItems = mutableListOf<MediaItem>()
            while (
                initialMediaItems.size < queueWindowSize &&
                nextFolderIndexToEnqueue < orderedFolderItems.size
            ) {
                initialMediaItems += buildMediaItem(nextFolderIndexToEnqueue)
                nextFolderIndexToEnqueue += 1
            }
            exoPlayer.setMediaItems(initialMediaItems, 0, 0L)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            refreshCurrentTrackMetadata()

            progressJob = viewModelScope.launch {
                while (true) {
                    refreshProgress()
                    delay(250)
                }
            }
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE) {
                exoPlayer.prepare()
            }
            exoPlayer.play()
        }
    }

    fun stopPlaybackForExit() {
        exoPlayer.pause()
        exoPlayer.seekTo(0L)
        exoPlayer.playWhenReady = false
        refreshProgress()
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

    private fun buildMediaItem(folderIndex: Int): MediaItem {
        val item = orderedFolderItems[folderIndex]
        val metadata = MediaMetadata.Builder()
            .setTitle(item.title)
            .setArtworkUri(item.artworkUri)
            .build()

        return MediaItem.Builder()
            .setMediaId(folderIndex.toString())
            .setUri(item.audioUri)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun trimConsumedQueueItems() {
        val currentIndex = exoPlayer.currentMediaItemIndex
        if (currentIndex <= 0) {
            return
        }
        exoPlayer.removeMediaItems(0, currentIndex)
    }

    private fun ensureQueueDepth() {
        if (orderedFolderItems.isEmpty()) {
            return
        }

        val desiredUpcomingDepth = queueWindowSize - 1
        var remainingUpcoming = exoPlayer.mediaItemCount - (exoPlayer.currentMediaItemIndex + 1)
        while (
            remainingUpcoming < desiredUpcomingDepth &&
            nextFolderIndexToEnqueue < orderedFolderItems.size
        ) {
            exoPlayer.addMediaItem(buildMediaItem(nextFolderIndexToEnqueue))
            nextFolderIndexToEnqueue += 1
            remainingUpcoming += 1
        }
    }

    private fun refreshCurrentTrackMetadata() {
        val mediaId = exoPlayer.currentMediaItem?.mediaId
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
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
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
