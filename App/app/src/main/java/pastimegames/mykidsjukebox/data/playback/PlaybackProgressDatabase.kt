package pastimegames.mykidsjukebox.data.playback

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PlaybackProgressEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PlaybackProgressDatabase : RoomDatabase() {
    abstract fun playbackProgressDao(): PlaybackProgressDao

    companion object {
        @Volatile
        private var instance: PlaybackProgressDatabase? = null

        fun getInstance(context: Context): PlaybackProgressDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PlaybackProgressDatabase::class.java,
                    "playback_progress.db"
                ).build().also { instance = it }
            }
        }
    }
}
