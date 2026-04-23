package pastimegames.mykidsjukebox.features.playerview

import android.content.Intent
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

class PlaybackService : MediaSessionService() {
    private lateinit var exoPlayer: ExoPlayer
    private var mediaSession: MediaSession? = null

    private var orderedFolderItems: List<PlayerQueueItem> = emptyList()
    private var safeStartIndex: Int = 0
    private var queueWindowSize: Int = 5
    private var nextFolderIndexToEnqueue: Int = 0

    private val servicePlayerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            ensureQueueDepth()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
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
            applyQueue(payload)
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    override fun onCreate() {
        super.onCreate()
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
        mediaSession?.run {
            player.removeListener(servicePlayerListener)
            release()
        }
        mediaSession = null
        exoPlayer.release()
        super.onDestroy()
    }

    private fun applyQueue(payload: QueueCommandPayload) {
        orderedFolderItems = payload.items
        safeStartIndex = payload.startIndex.coerceIn(0, (orderedFolderItems.size - 1).coerceAtLeast(0))
        queueWindowSize = payload.queueWindowSize.coerceAtLeast(2)
        nextFolderIndexToEnqueue = safeStartIndex

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
}
