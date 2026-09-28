package com.smish.wheresmymoney.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val PixelColorScheme = lightColorScheme(
    primary = PixelInk,
    onPrimary = PixelBackground,
    secondary = PixelGreen,
    onSecondary = PixelInk,
    background = PixelBackground,
    onBackground = PixelInk,
    surface = PixelSurface,
    onSurface = PixelInk,
    error = PixelRed,
    onError = PixelSurface,
    onSurfaceVariant = PixelInkLight
)

private val DarkPixelColorScheme = darkColorScheme(
    primary = PixelDarkInk,
    onPrimary = PixelDarkBackground,
    secondary = PixelGreen,
    onSecondary = PixelDarkBackground,
    background = PixelDarkBackground,
    onBackground = PixelDarkInk,
    surface = PixelDarkSurface,
    onSurface = PixelDarkInk,
    error = PixelRed,
    onError = PixelDarkSurface,
    onSurfaceVariant = PixelDarkInkLight
)

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkPixelColorScheme else PixelColorScheme

    CompositionLocalProvider(
        LocalSpacing provides Spacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PixelTypography,
            shapes = PixelShapes,
            content = content
        )
    }
}

object ExpenseTrackerTheme {
    val spacing: Spacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current
}
