package pastimegames.mykidsjukebox.features.libraryoverview.components

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
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
    val shape = RoundedCornerShape(14.dp)
    val imageRequest = artworkUri?.let {
        ImageRequest.Builder(LocalContext.current)
            .data(it)
            .crossfade(300)
            .build()
    }
    val artworkPainter = imageRequest?.let { rememberAsyncImagePainter(model = it) }
    val painterState = artworkPainter?.state

    val artworkLoaded = painterState is AsyncImagePainter.State.Success
    val shouldShimmer = artworkIsLoading || (artworkUri != null && painterState is AsyncImagePainter.State.Loading)

    val placeholderAlpha by animateFloatAsState(
        targetValue = if (artworkLoaded) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "artwork-placeholder-alpha"
    )
    val artworkAlpha by animateFloatAsState(
        targetValue = if (artworkLoaded) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "artwork-image-alpha"
    )

    val placeholderModifier = if (shouldShimmer) {
        Modifier
            .fillMaxSize()
            .shimmerPlaceholder(shape)
    } else {
        Modifier
            .fillMaxSize()
            .background(color = colorScheme.tertiary, shape = shape)
    }

    Box(modifier = modifier.clip(shape)) {
        Box(
            modifier = placeholderModifier.alpha(placeholderAlpha),
            contentAlignment = Alignment.Center
        ) {
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

        if (artworkPainter != null) {
            Image(
                painter = artworkPainter,
                contentDescription = stringResource(R.string.folder_artwork_description),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(artworkAlpha)
            )
        }
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
