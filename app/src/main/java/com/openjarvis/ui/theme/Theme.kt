package com.openjarvis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val JarvisDarkColors = darkColorScheme(
    primary = VoidColor.Violet,
    secondary = VoidColor.Cyan,
    tertiary = VoidColor.Green,
    background = VoidColor.Void950,
    surface = VoidColor.Void900,
    onPrimary = VoidColor.TextPrimary,
    onSecondary = VoidColor.Void950,
    onBackground = VoidColor.TextPrimary,
    onSurface = VoidColor.TextPrimary
)

@Composable
fun OpenJarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JarvisDarkColors,
        content = content
    )
}
