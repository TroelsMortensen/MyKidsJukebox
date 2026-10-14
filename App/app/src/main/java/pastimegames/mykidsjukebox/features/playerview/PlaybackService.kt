package pastimegames.mykidsjukebox.features.playerview

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ControllerInfo
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pastimegames.mykidsjukebox.data.playback.PlaybackProgressRepository

class PlaybackService : MediaSessionService() {
    private lateinit var exoPlayer: ExoPlayer
    private var mediaSession: MediaSession? = null
    private lateinit var progressRepository: PlaybackProgressRepository

    private var orderedFolderItems: List<PlayerQueueItem> = emptyList()
    private var safeStartIndex: Int = 0
    private var queueWindowSize: Int = 5
    private var nextFolderIndexToEnqueue: Int = 0

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressSaveJob: Job? = null
    private var lastTrackedAudioUri: Uri? = null
    private var lastKnownPositionMs: Long = 0L
    private var lastKnownDurationMs: Long = 0L

    private val servicePlayerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            ensureQueueDepth()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                startPeriodicProgressSaving()
            } else {
                stopPeriodicProgressSaving()
                flushCurrentProgress()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            when (reason) {
                Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> {
                    val finishedUri = lastTrackedAudioUri
                    if (finishedUri != null) {
                        serviceScope.launch(Dispatchers.IO) {
                            progressRepository.clearProgress(finishedUri.toString())
                        }
                    }
                    resumeIncomingItem(mediaItem)
                }

                Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> {
                    val previousUri = lastTrackedAudioUri
                    if (previousUri != null) {
                        flushProgressForUri(
                            audioUri = previousUri,
                            positionMs = lastKnownPositionMs,
                            durationMs = lastKnownDurationMs
                        )
                    }
                    resumeIncomingItem(mediaItem)
                }

                else -> {
                    lastTrackedAudioUri = resolveAudioUri(mediaItem)
                    captureProgressSnapshot()
                }
            }
            trimConsumedQueueItems()
            ensureQueueDepth()
        }
    }

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val sessionCommands = connectionResult.availableSessionCommands
                .buildUpon()
                .add(SessionCommand(COMMAND_SET_QUEUE, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.accept(
                sessionCommands,
                connectionResult.availablePlayerCommands
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction != COMMAND_SET_QUEUE) {
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
            }
            val payload = args.toQueueCommandPayloadOrNull()
                ?: return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE))
            serviceScope.launch {
                applyQueue(payload)
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    override fun onCreate() {
        super.onCreate()
        progressRepository = PlaybackProgressRepository(this)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        exoPlayer.addListener(servicePlayerListener)
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setCallback(sessionCallback)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!exoPlayer.isPlaying) {
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        stopPeriodicProgressSaving()
        flushCurrentProgress()
        serviceScope.cancel()
        mediaSession?.run {
            player.removeListener(servicePlayerListener)
            release()
        }
        mediaSession = null
        exoPlayer.release()
        super.onDestroy()
    }

    private suspend fun applyQueue(payload: QueueCommandPayload) {
        flushCurrentProgress()

        orderedFolderItems = payload.items
        safeStartIndex = payload.startIndex.coerceIn(0, (orderedFolderItems.size - 1).coerceAtLeast(0))
        queueWindowSize = payload.queueWindowSize.coerceAtLeast(2)
        nextFolderIndexToEnqueue = safeStartIndex

        val startItem = orderedFolderItems.getOrNull(safeStartIndex)
        val startPositionMs = if (startItem != null) {
            withContext(Dispatchers.IO) {
                progressRepository.getPositionMs(startItem.audioUri.toString()).also {
                    progressRepository.pruneKeepNewest(PlaybackProgressRepository.MAX_TRACKED_BOOKS)
                }
            }
        } else {
            0L
        }

        val initialMediaItems = mutableListOf<MediaItem>()
        while (
            initialMediaItems.size < queueWindowSize &&
            nextFolderIndexToEnqueue < orderedFolderItems.size
        ) {
            initialMediaItems += buildMediaItem(nextFolderIndexToEnqueue)
            nextFolderIndexToEnqueue += 1
        }

        exoPlayer.setMediaItems(initialMediaItems, 0, startPositionMs)
        lastTrackedAudioUri = startItem?.audioUri
        lastKnownPositionMs = startPositionMs
        lastKnownDurationMs = 0L
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    private fun buildMediaItem(folderIndex: Int): MediaItem {
        val item = orderedFolderItems[folderIndex]
        val metadata = MediaMetadata.Builder()
            .setTitle(item.title)
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

    private fun resumeIncomingItem(mediaItem: MediaItem?) {
        val newUri = resolveAudioUri(mediaItem)
        lastTrackedAudioUri = newUri
        lastKnownPositionMs = 0L
        lastKnownDurationMs = 0L
        if (newUri == null) {
            return
        }
        val expectedMediaId = mediaItem?.mediaId
        serviceScope.launch {
            val resumePositionMs = withContext(Dispatchers.IO) {
                progressRepository.getPositionMs(newUri.toString()).also {
                    progressRepository.pruneKeepNewest(PlaybackProgressRepository.MAX_TRACKED_BOOKS)
                }
            }
            if (resumePositionMs > 0L &&
                exoPlayer.currentMediaItem?.mediaId == expectedMediaId
            ) {
                exoPlayer.seekTo(resumePositionMs)
                lastKnownPositionMs = resumePositionMs
            }
        }
    }

    private fun startPeriodicProgressSaving() {
        if (progressSaveJob?.isActive == true) {
            return
        }
        progressSaveJob = serviceScope.launch {
            while (isActive) {
                delay(PROGRESS_SAVE_INTERVAL_MS)
                flushCurrentProgress()
            }
        }
    }

    private fun stopPeriodicProgressSaving() {
        progressSaveJob?.cancel()
        progressSaveJob = null
    }

    private fun captureProgressSnapshot() {
        lastKnownPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
        lastKnownDurationMs = exoPlayer.duration.takeIf { it > 0L } ?: 0L
        lastTrackedAudioUri = resolveCurrentAudioUri() ?: lastTrackedAudioUri
    }

    private fun flushCurrentProgress() {
        captureProgressSnapshot()
        val audioUri = lastTrackedAudioUri ?: return
        flushProgressForUri(
            audioUri = audioUri,
            positionMs = lastKnownPositionMs,
            durationMs = lastKnownDurationMs
        )
    }

    private fun flushProgressForUri(
        audioUri: Uri,
        positionMs: Long,
        durationMs: Long
    ) {
        serviceScope.launch {
            withContext(Dispatchers.IO) {
                progressRepository.saveProgress(
                    audioUri = audioUri.toString(),
                    positionMs = positionMs,
                    durationMs = durationMs
                )
            }
        }
    }

    private fun resolveCurrentAudioUri(): Uri? {
        return resolveAudioUri(exoPlayer.currentMediaItem)
    }

    private fun resolveAudioUri(mediaItem: MediaItem?): Uri? {
        val folderIndex = mediaItem?.mediaId?.toIntOrNull() ?: return null
        return orderedFolderItems.getOrNull(folderIndex)?.audioUri
    }

    companion object {
        private const val PROGRESS_SAVE_INTERVAL_MS = 10_000L
    }
}
