package dev.nausheen.smartspend.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    secondary = EmeraldDark,
    background = Base,
    onBackground = Charcoal,
    surface = Surface,
    onSurface = Charcoal,
    surfaceVariant = Color(0xFFECEEEC),
    onSurfaceVariant = Charcoal.copy(alpha = 0.6f),
    error = Danger,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    background = DarkBase,
    onBackground = OffWhite,
    surface = DarkSurface,
    onSurface = OffWhite,
    error = Danger,
)

@Composable
fun SmartSpendTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Intentional brand palette — no dynamic color, so it looks the same everywhere.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
