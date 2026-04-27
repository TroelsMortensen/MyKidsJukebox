package pastimegames.mykidsjukebox.features.playerview

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            currentAudioUri = orderedFolderItems.getOrNull(safeStartIndex)?.audioUri,
            artworkUri = null,
            upcomingItems = orderedFolderItems.drop(safeStartIndex + 1).take(4),
            artworkByAudioUri = emptyMap(),
            isPlaying = false,
            durationMs = 0L,
            positionMs = 0L
        )
    )
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var progressJob: Job? = null
    private var mediaController: MediaController? = null
    private var hasSentInitialQueue = false
    private val artworkByAudioUriCache = mutableMapOf<String, Uri?>()
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
        val upcomingItems = orderedFolderItems.drop(folderIndex + 1).take(4)

        _state.update { current ->
            current.copy(
                title = currentItem.title,
                currentAudioUri = currentItem.audioUri,
                artworkUri = artworkByAudioUriCache[currentItem.audioUri.toString()],
                upcomingItems = upcomingItems,
                artworkByAudioUri = artworkByAudioUriCache.toMap()
            )
        }
        resolveAndCacheArtwork(currentItem.audioUri)
        upcomingItems.forEach { resolveAndCacheArtwork(it.audioUri) }
    }

    private fun resolveAndCacheArtwork(audioUri: Uri) {
        val audioKey = audioUri.toString()
        if (artworkByAudioUriCache.containsKey(audioKey)) {
            return
        }
        viewModelScope.launch {
            val resolvedArtworkUri = withContext(Dispatchers.IO) {
                resolveArtworkFromAudioUri(audioUri)
            }
            artworkByAudioUriCache[audioKey] = resolvedArtworkUri
            _state.update { current ->
                if (current.currentAudioUri == audioUri) {
                    current.copy(
                        artworkUri = resolvedArtworkUri,
                        artworkByAudioUri = artworkByAudioUriCache.toMap()
                    )
                } else {
                    current.copy(artworkByAudioUri = artworkByAudioUriCache.toMap())
                }
            }
        }
    }

    private fun resolveArtworkFromAudioUri(audioUri: Uri): Uri? {
        val app = getApplication<Application>()
        if (!DocumentsContract.isDocumentUri(app, audioUri)) {
            return null
        }

        val documentId = try {
            DocumentsContract.getDocumentId(audioUri)
        } catch (_: IllegalArgumentException) {
            return null
        }
        val parentDocumentId = documentId.substringBeforeLast('/', missingDelimiterValue = "")
        if (parentDocumentId.isBlank()) {
            return null
        }

        val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(audioUri, parentDocumentId)
        val parentFolder = DocumentFile.fromTreeUri(app, parentDocumentUri)
            ?: DocumentFile.fromSingleUri(app, parentDocumentUri)
            ?: return null
        val siblings = parentFolder.listFiles()
        val audioName = siblings.firstOrNull { it.uri == audioUri }?.name ?: return null
        val baseName = audioName.substringBeforeLast('.', missingDelimiterValue = audioName)
        val artworkCandidates = listOf(
            "$baseName.jpg",
            "$baseName.jpeg",
            "$baseName.png",
            "$baseName.webp",
            "cover.jpg",
            "cover.jpeg",
            "cover.png",
            "artwork.jpg",
            "artwork.jpeg",
            "artwork.png"
        )
        val siblingFiles = siblings.filter { it.isFile }
        for (candidate in artworkCandidates) {
            val match = siblingFiles.firstOrNull { sibling ->
                sibling.name.equals(candidate, ignoreCase = true)
            }
            if (match != null) {
                return match.uri
            }
        }
        return null
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
