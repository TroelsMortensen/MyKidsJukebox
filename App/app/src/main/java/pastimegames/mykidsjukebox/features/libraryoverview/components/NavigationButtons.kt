package pastimegames.mykidsjukebox.features.libraryoverview.components

import androidx.compose.runtime.Composable
import pastimegames.mykidsjukebox.features.shared.components.LargeCloseButton

@Composable
fun NavigationButtons(
    showBackButton: Boolean,
    onBackClick: () -> Unit
) {
    if (showBackButton) {
        LargeCloseButton(onClick = onBackClick)
    }
}
