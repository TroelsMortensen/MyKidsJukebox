package pastimegames.mykidsjukebox.data.playback

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey val audioUri: String,
    val positionMs: Long,
    val updatedAt: Long
)
