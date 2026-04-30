package com.example.composeplayground.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// ── Home color palette ────────────────────────────────────────────────────────
// Deep navy / midnight-blue aesthetic — neutral enough to frame both the
// Pokémon (red/yellow) and Picsum (blue/cyan) gradient cards without clashing.

private val HomeLightColorScheme = lightColorScheme(
    primary = Color(0xFF1A3A6E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E4FF),
    onPrimaryContainer = Color(0xFF001847),
    secondary = Color(0xFF575E71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDBE1F9),
    onSecondaryContainer = Color(0xFF131C2B),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474E),
    surfaceContainer = Color(0xFFECEDF5),
    outline = Color(0xFF74777F),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val HomeDarkColorScheme = darkColorScheme(
    primary = Color(0xFFADC6FF),
    onPrimary = Color(0xFF002E6C),
    primaryContainer = Color(0xFF00429A),
    onPrimaryContainer = Color(0xFFD8E4FF),
    secondary = Color(0xFFBFC6DC),
    onSecondary = Color(0xFF293141),
    secondaryContainer = Color(0xFF3F4759),
    onSecondaryContainer = Color(0xFFDBE1F9),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    surfaceContainer = Color(0xFF1D2024),
    outline = Color(0xFF8E9099),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/**
 * Deep navy Material3 theme for the Home menu screen.
 *
 * Dark/light mode is inferred from the parent [MaterialTheme]'s background
 * luminance so that [ThemeViewModel]'s force-dark/light preference propagates
 * without threading an extra parameter through the navigation stack.
 */
@Composable
fun HomeTheme(
    darkTheme: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) HomeDarkColorScheme else HomeLightColorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
