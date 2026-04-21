package pastimegames.mykidsjukebox.features.libraryoverview.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import pastimegames.mykidsjukebox.data.library.LibraryItemKind

@Composable
fun FolderArtwork(
    artworkUri: Uri?,
    itemKind: LibraryItemKind,
    modifier: Modifier = Modifier
) {
    if (artworkUri != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artworkUri)
                .crossfade(true)
                .build(),
            contentDescription = "Folder artwork",
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
        return
    }

    Box(
        modifier = modifier
            .background(color = Color(0xFFFBBF24), shape = RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        val (icon, iconTint) = when (itemKind) {
            LibraryItemKind.Folder -> Icons.Filled.Folder to Color(0xFF1D4ED8)
            LibraryItemKind.Audio -> Icons.Filled.Audiotrack to Color(0xFFB91C1C)
        }
        Icon(
            imageVector = icon,
            contentDescription = if (itemKind == LibraryItemKind.Folder) "Folder" else "Audiobook",
            tint = iconTint,
            modifier = Modifier.size(56.dp)
        )
    }
}
