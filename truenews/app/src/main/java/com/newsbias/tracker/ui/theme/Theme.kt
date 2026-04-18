package com.newsbias.tracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary        = Color(0xFF82B1FF),
    onPrimary      = Color(0xFF001F6B),
    background     = DarkBg,
    onBackground   = Color(0xFFE0E0E0),
    surface        = DarkSurface,
    onSurface      = Color(0xFFE0E0E0),
    surfaceVariant = DarkSurface2,
    error          = FakeHigh,
)

@Composable
fun NewsBiasTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            content()
        }
    }
}