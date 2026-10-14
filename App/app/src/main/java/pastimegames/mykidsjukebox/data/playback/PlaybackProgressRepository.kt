package pastimegames.mykidsjukebox.data.playback

import android.content.Context

class PlaybackProgressRepository(context: Context) {
    private val dao = PlaybackProgressDatabase.getInstance(context).playbackProgressDao()

    suspend fun getPositionMs(audioUri: String): Long {
        return dao.get(audioUri)?.positionMs?.coerceAtLeast(0L) ?: 0L
    }

    suspend fun saveProgress(
        audioUri: String,
        positionMs: Long,
        durationMs: Long
    ) {
        val safePosition = positionMs.coerceAtLeast(0L)
        if (safePosition < MIN_SAVE_POSITION_MS) {
            dao.delete(audioUri)
            return
        }
        if (durationMs > 0L) {
            val remainingMs = durationMs - safePosition
            val progressFraction = safePosition.toFloat() / durationMs.toFloat()
            if (remainingMs <= NEAR_END_REMAINING_MS || progressFraction >= NEAR_END_FRACTION) {
                dao.delete(audioUri)
                return
            }
        }
        dao.upsert(
            PlaybackProgressEntity(
                audioUri = audioUri,
                positionMs = safePosition,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearProgress(audioUri: String) {
        dao.delete(audioUri)
    }

    suspend fun pruneKeepNewest(limit: Int = MAX_TRACKED_BOOKS) {
        dao.pruneKeepNewest(limit)
    }

    companion object {
        const val MAX_TRACKED_BOOKS = 20
        private const val MIN_SAVE_POSITION_MS = 3_000L
        private const val NEAR_END_REMAINING_MS = 5_000L
        private const val NEAR_END_FRACTION = 0.95f
    }
}
