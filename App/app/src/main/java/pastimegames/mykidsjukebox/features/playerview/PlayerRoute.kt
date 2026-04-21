package pastimegames.mykidsjukebox.features.playerview

import android.net.Uri

sealed interface PlayerRoute {
    data object Library : PlayerRoute

    data class Player(
        val title: String,
        val audioUri: Uri,
        val artworkUri: Uri?,
        val sessionId: Long
    ) : PlayerRoute
}
