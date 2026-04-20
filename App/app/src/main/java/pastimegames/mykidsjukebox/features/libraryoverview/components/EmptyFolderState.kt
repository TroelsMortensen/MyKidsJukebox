package pastimegames.mykidsjukebox.features.libraryoverview.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

@Composable
fun EmptyFolderState(hasBrowsableContent: Boolean) {
    Text(
        text = if (hasBrowsableContent) {
            "No subfolders here. Go back or choose another folder."
        } else {
            "This folder is empty."
        },
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}
