package com.dhobigo.tracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = ForestGreenPrimary,
    onPrimary = SurfacePureWhite,
    primaryContainer = ForestGreenContainer,
    onPrimaryContainer = ForestGreenOnContainer,
    secondary = ForestGreenLight,
    onSecondary = SurfacePureWhite,
    secondaryContainer = MintSoftBg,
    onSecondaryContainer = ForestGreenPrimary,
    tertiary = MintAccent,
    onTertiary = SurfacePureWhite,
    background = WarmCreamBg,
    onBackground = TextForestPrimary,
    surface = SurfacePureWhite,
    onSurface = TextForestPrimary,
    surfaceVariant = SurfaceWarmSubtle,
    onSurfaceVariant = TextMutedSecondary,
    outline = CardBorderSubtle,
    outlineVariant = CardBorderFocused,
    error = DangerRed,
    errorContainer = DangerRedBg,
    onError = SurfacePureWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBackground,
    primaryContainer = ForestGreenDark,
    onPrimaryContainer = DarkTextPrimary,
    secondary = MintLight,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceSubtle,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = MintAccent,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceSubtle,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = DangerRed,
    errorContainer = DangerRedBg,
    onError = SurfacePureWhite
)

@Composable
fun DhobiTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
