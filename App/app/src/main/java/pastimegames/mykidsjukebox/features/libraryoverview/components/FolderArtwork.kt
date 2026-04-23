package pastimegames.mykidsjukebox.features.libraryoverview.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
    artworkIsLoading: Boolean,
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

    val shape = RoundedCornerShape(14.dp)
    val placeholderModifier = if (artworkIsLoading) {
        modifier.shimmerPlaceholder(shape)
    } else {
        modifier.background(color = Color(0xFFFBBF24), shape = shape)
    }

    Box(modifier = placeholderModifier, contentAlignment = Alignment.Center) {
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

@Composable
private fun Modifier.shimmerPlaceholder(shape: RoundedCornerShape): Modifier {
    val transition = rememberInfiniteTransition(label = "artwork-placeholder-shimmer")
    val shimmerOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "artwork-placeholder-shimmer-offset"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF59E0B),
            Color(0xFFFCD34D),
            Color(0xFFF59E0B)
        ),
        start = Offset(shimmerOffset - 250f, 0f),
        end = Offset(shimmerOffset, 250f)
    )
    return this
        .clip(shape)
        .background(brush = shimmerBrush)
}
