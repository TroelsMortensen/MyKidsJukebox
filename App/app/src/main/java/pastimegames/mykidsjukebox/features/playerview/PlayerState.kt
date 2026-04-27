package pastimegames.mykidsjukebox.features.playerview

import android.net.Uri

data class PlayerState(
    val title: String,
    val currentAudioUri: Uri?,
    val artworkUri: Uri?,
    val upcomingItems: List<PlayerQueueItem>,
    val artworkByAudioUri: Map<String, Uri?>,
    val isPlaying: Boolean,
    val durationMs: Long,
    val positionMs: Long
) {
    val remainingMs: Long
        get() = (durationMs - positionMs).coerceAtLeast(0L)

    val progress: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}
