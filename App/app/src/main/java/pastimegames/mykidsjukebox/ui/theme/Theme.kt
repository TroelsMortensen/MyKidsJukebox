package pastimegames.mykidsjukebox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CozyLeaf,
    onPrimary = CozyOnPrimary,
    secondary = CozyBerry,
    tertiary = CozyHoney,
    background = Color(0xFF201B17),
    onBackground = Color(0xFFF3EADF),
    surface = Color(0xFF2B2420),
    onSurface = Color(0xFFF3EADF)
)

private val LightColorScheme = lightColorScheme(
    primary = CozyCoral,
    onPrimary = CozyOnPrimary,
    secondary = CozyLeaf,
    onSecondary = CozyOnPrimary,
    tertiary = CozyBerry,
    onTertiary = CozyOnPrimary,
    background = CozyCream,
    onBackground = CozyInk,
    surface = CozyPaper,
    onSurface = CozyInk,
    surfaceVariant = CozySky,
    onSurfaceVariant = CozyInkMuted,
    error = CozyRose,
    onError = CozyOnPrimary
)

@Composable
fun MyKidsJukeboxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}