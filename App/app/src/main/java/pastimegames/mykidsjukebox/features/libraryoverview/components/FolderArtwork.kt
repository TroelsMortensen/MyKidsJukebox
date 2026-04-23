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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import pastimegames.mykidsjukebox.R
import pastimegames.mykidsjukebox.data.library.LibraryItemKind
import pastimegames.mykidsjukebox.ui.theme.CozyCoral
import pastimegames.mykidsjukebox.ui.theme.CozyHoney
import pastimegames.mykidsjukebox.ui.theme.CozyLeaf

@Composable
fun FolderArtwork(
    artworkUri: Uri?,
    artworkIsLoading: Boolean,
    itemKind: LibraryItemKind,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    if (artworkUri != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artworkUri)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.folder_artwork_description),
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
        return
    }

    val shape = RoundedCornerShape(14.dp)
    val placeholderModifier = if (artworkIsLoading) {
        modifier.shimmerPlaceholder(shape)
    } else {
        modifier.background(color = colorScheme.tertiary, shape = shape)
    }

    Box(modifier = placeholderModifier, contentAlignment = Alignment.Center) {
        val (icon, iconTint) = when (itemKind) {
            LibraryItemKind.Folder -> Icons.Filled.Folder to colorScheme.primary
            LibraryItemKind.Audio -> Icons.Filled.Audiotrack to colorScheme.error
        }
        Icon(
            imageVector = icon,
            contentDescription = if (itemKind == LibraryItemKind.Folder) {
                stringResource(R.string.folder_icon_description)
            } else {
                stringResource(R.string.audiobook_icon_description)
            },
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
            CozyHoney,
            CozyLeaf,
            CozyCoral
        ),
        start = Offset(shimmerOffset - 250f, 0f),
        end = Offset(shimmerOffset, 250f)
    )
    return this
        .clip(shape)
        .background(brush = shimmerBrush)
}
