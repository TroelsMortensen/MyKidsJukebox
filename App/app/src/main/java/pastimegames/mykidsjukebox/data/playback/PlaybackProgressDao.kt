package pastimegames.mykidsjukebox.data.playback

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PlaybackProgressDao {
    @Query("SELECT * FROM playback_progress WHERE audioUri = :audioUri LIMIT 1")
    suspend fun get(audioUri: String): PlaybackProgressEntity?

    @Upsert
    suspend fun upsert(entity: PlaybackProgressEntity)

    @Query("DELETE FROM playback_progress WHERE audioUri = :audioUri")
    suspend fun delete(audioUri: String)

    @Query(
        """
        DELETE FROM playback_progress
        WHERE audioUri NOT IN (
            SELECT audioUri FROM playback_progress
            ORDER BY updatedAt DESC
            LIMIT :limit
        )
        """
    )
    suspend fun pruneKeepNewest(limit: Int)
}
