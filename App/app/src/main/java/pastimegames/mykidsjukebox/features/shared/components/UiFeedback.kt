package pastimegames.mykidsjukebox.features.shared.components

import android.view.SoundEffectConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

@Stable
class UiFeedback(
    private val playClick: () -> Unit,
    private val hapticFeedback: HapticFeedback
) {
    fun performPrimaryActionFeedback() {
        playClick()
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun performSuccessFeedback() {
        playClick()
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun performErrorFeedback() {
        playClick()
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

@Composable
fun rememberUiFeedback(): UiFeedback {
    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current
    return remember(view, hapticFeedback) {
        UiFeedback(
            playClick = { view.playSoundEffect(SoundEffectConstants.CLICK) },
            hapticFeedback = hapticFeedback
        )
    }
}
