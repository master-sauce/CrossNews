package com.newsbias.tracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = NewsRed,
    onPrimary = OnDark,
    primaryContainer = NewsRedDark,
    onPrimaryContainer = OnDark,
    secondary = NewsRedLight,
    background = DarkBg,
    onBackground = OnDark,
    surface = DarkSurface,
    onSurface = OnDark,
    surfaceVariant = DarkSurface2,
    onSurfaceVariant = OnSurface2,
    outline = DarkBorder,
    error = FakeHigh,
)

private val LightColors = lightColorScheme(
    primary = NewsRed,
    onPrimary = OnDark,
    primaryContainer = NewsRedLight,
    onPrimaryContainer = OnDark,
    secondary = NewsRedDark,
    background = LightBg,
    onBackground = OnLight,
    surface = LightSurface,
    onSurface = OnLight,
    surfaceVariant = LightSurface2,
    onSurfaceVariant = OnLight2,
    outline = LightBorder,
    error = FakeHigh,
)

@Composable
fun NewsBiasTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}