package pastimegames.mykidsjukebox.features.playerview.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import pastimegames.mykidsjukebox.R

@Composable
internal fun PlayerArtwork(
    artworkUri: Uri?,
    modifier: Modifier = Modifier
) {
    if (artworkUri != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artworkUri)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.current_audio_artwork_description),
            contentScale = ContentScale.Fit,
            modifier = modifier.aspectRatio(1f)
        )
        return
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(18.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Audiotrack,
            contentDescription = stringResource(R.string.default_audio_artwork_description),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(96.dp)
        )
    }
}
