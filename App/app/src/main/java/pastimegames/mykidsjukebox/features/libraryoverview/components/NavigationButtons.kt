package pastimegames.mykidsjukebox.features.libraryoverview.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NavigationButtons(
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    onParentalSettingsClick: () -> Unit
) {
    if (showBackButton) {
        Button(
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            Text(
                text = "Back",
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

    Button(
        onClick = onParentalSettingsClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Text("Parental Settings")
    }
}
