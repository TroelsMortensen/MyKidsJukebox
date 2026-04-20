package pastimegames.mykidsjukebox.features.libraryoverview.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SelectFolderState(
    onSelectFolderClick: () -> Unit,
    errorMessage: String?
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Select your library folder to begin.",
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onSelectFolderClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Text("Select Library Folder")
            }
            errorMessage?.let {
                Text(text = it, textAlign = TextAlign.Center)
            }
        }
    }
}
