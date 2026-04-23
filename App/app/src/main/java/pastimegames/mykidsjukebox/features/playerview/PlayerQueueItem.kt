package pastimegames.mykidsjukebox.features.playerview

import android.net.Uri

data class PlayerQueueItem(
    val title: String,
    val audioUri: Uri,
    val artworkUri: Uri?
)
