package pastimegames.mykidsjukebox.features.playerview.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal data class PlayerLayoutTokens(
    val contentPadding: Dp,
    val sectionSpacing: Dp,
    val closeButtonHeight: Dp,
    val closeIconSize: Dp,
    val closeCornerRadius: Dp,
    val artworkHeightFraction: Float,
    val artworkWidthFraction: Float,
    val timelineTextWidth: Dp,
    val timelineItemSpacing: Dp,
    val timelineHeight: Dp,
    val controlsSpacing: Dp,
    val playButtonHeight: Dp,
    val playIconSize: Dp,
    val queueSectionSpacing: Dp,
    val queueItemBaseSize: Dp,
    val queueItemMinSize: Dp,
    val queueItemShrinkStep: Dp,
    val queueItemSpacing: Dp,
    val queueCornerRadius: Dp,
    val queueIconSize: Dp
)

internal fun buildPlayerLayoutTokens(
    maxWidth: Dp,
    maxHeight: Dp
): PlayerLayoutTokens {
    val isWideLayout = maxWidth > maxHeight

    val closeButtonHeight = (maxWidth * 0.14f).coerceIn(100.dp, 136.dp)
    val playButtonHeight = (maxHeight * if (isWideLayout) 0.18f else 0.24f).coerceIn(100.dp, 220.dp)
    val queueItemBaseSize = (maxWidth * if (isWideLayout) 0.14f else 0.20f).coerceIn(72.dp, 148.dp)

    return PlayerLayoutTokens(
        contentPadding = (maxWidth * 0.04f).coerceIn(16.dp, 32.dp),
        sectionSpacing = (maxHeight * 0.02f).coerceIn(12.dp, 24.dp),
        closeButtonHeight = closeButtonHeight,
        closeIconSize = (closeButtonHeight * 0.65f).coerceIn(56.dp, 88.dp),
        closeCornerRadius = closeButtonHeight / 2f,
        artworkHeightFraction = if (isWideLayout) 0.72f else 0.78f,
        artworkWidthFraction = if (isWideLayout) 0.75f else 0.9f,
        timelineTextWidth = (maxWidth * 0.12f).coerceIn(64.dp, 104.dp),
        timelineItemSpacing = (maxWidth * 0.015f).coerceIn(8.dp, 14.dp),
        timelineHeight = (maxWidth * 0.012f).coerceIn(8.dp, 14.dp),
        controlsSpacing = (maxHeight * 0.01f).coerceIn(6.dp, 14.dp),
        playButtonHeight = playButtonHeight,
        playIconSize = (playButtonHeight * 0.82f).coerceIn(72.dp, 188.dp),
        queueSectionSpacing = (maxHeight * 0.01f).coerceIn(8.dp, 14.dp),
        queueItemBaseSize = queueItemBaseSize,
        queueItemMinSize = 56.dp,
        queueItemShrinkStep = (queueItemBaseSize * 0.1f).coerceIn(4.dp, 12.dp),
        queueItemSpacing = (maxWidth * 0.02f).coerceIn(8.dp, 16.dp),
        queueCornerRadius = (queueItemBaseSize * 0.12f).coerceIn(10.dp, 18.dp),
        queueIconSize = (queueItemBaseSize * 0.36f).coerceIn(28.dp, 56.dp)
    )
}
