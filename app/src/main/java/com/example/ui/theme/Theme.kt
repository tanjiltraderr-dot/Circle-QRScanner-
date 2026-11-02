package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Pure White Clean Theme
private val PureWhiteColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = AccentBlueContainer,
    onPrimaryContainer = AccentBlueDark,
    secondary = AccentGreen,
    onSecondary = Color.White,
    secondaryContainer = AccentGreenContainer,
    onSecondaryContainer = AccentGreenDark,
    tertiary = AmberGold,
    background = PureWhite,
    surface = PureWhite,
    surfaceVariant = OffWhite,
    outline = SurfaceBorder,
    outlineVariant = SurfaceBorderLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Pure white theme requested
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = PureWhiteColorScheme,
        typography = Typography,
        content = content
    )
}
