package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val BlockCrushColorScheme = lightColorScheme(
    primary = M3Primary,
    onPrimary = M3OnPrimary,
    primaryContainer = M3PrimaryContainer,
    onPrimaryContainer = M3OnPrimaryContainer,
    secondary = M3Secondary,
    onSecondary = M3OnSecondary,
    tertiary = M3Tertiary,
    background = WoodBackgroundTop,
    onBackground = CreamText,
    surface = M3Surface,
    onSurface = M3OnSurface,
)

private val BlockCrushShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BlockCrushTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BlockCrushColorScheme,
        typography = Typography,
        shapes = BlockCrushShapes,
        content = content
    )
}
