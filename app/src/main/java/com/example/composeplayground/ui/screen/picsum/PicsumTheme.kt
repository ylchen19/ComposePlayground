package com.example.composeplayground.ui.screen.picsum

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// ── Picsum color palette ─────────────────────────────────────────────────────
// Warm analog-photography aesthetic: amber, cream, and dark-room tones.

private val PicsumLightColorScheme = lightColorScheme(
    primary = Color(0xFF7B4F1A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDB3),
    onPrimaryContainer = Color(0xFF2B1700),
    secondary = Color(0xFF6D5C4F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF7DDD2),
    onSecondaryContainer = Color(0xFF261914),
    background = Color(0xFFFFF8F4),
    onBackground = Color(0xFF201A17),
    surface = Color(0xFFFFF8F4),
    onSurface = Color(0xFF201A17),
    surfaceVariant = Color(0xFFF3DDD1),
    onSurfaceVariant = Color(0xFF51443D),
    surfaceContainer = Color(0xFFF5E6DB),
    outline = Color(0xFF85736C),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val PicsumDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFBA6D),
    onPrimary = Color(0xFF462A00),
    primaryContainer = Color(0xFF643E00),
    onPrimaryContainer = Color(0xFFFFDDB3),
    secondary = Color(0xFFDCBFB4),
    onSecondary = Color(0xFF3E2D27),
    secondaryContainer = Color(0xFF56433C),
    onSecondaryContainer = Color(0xFFF7DDD2),
    background = Color(0xFF17110C),
    onBackground = Color(0xFFEDE0D9),
    surface = Color(0xFF17110C),
    onSurface = Color(0xFFEDE0D9),
    surfaceVariant = Color(0xFF51443D),
    onSurfaceVariant = Color(0xFFD5C3BB),
    surfaceContainer = Color(0xFF261D17),
    outline = Color(0xFF9E8D85),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/**
 * Warm photography-inspired Material3 theme for the Picsum feature.
 *
 * Dark/light mode is inferred from the parent [MaterialTheme]'s background
 * luminance so that [ThemeViewModel]'s force-dark/light preference propagates
 * without threading an extra parameter through the navigation stack.
 */
@Composable
fun PicsumTheme(
    darkTheme: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) PicsumDarkColorScheme else PicsumLightColorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
