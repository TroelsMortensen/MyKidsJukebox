package pastimegames.mykidsjukebox.features.playerview

sealed interface PlayerRoute {
    data object Library : PlayerRoute

    data class Player(
        val folderAudioItems: List<PlayerQueueItem>,
        val startIndex: Int,
        val initialWindowSize: Int,
        val sessionId: Long
    ) : PlayerRoute
}
