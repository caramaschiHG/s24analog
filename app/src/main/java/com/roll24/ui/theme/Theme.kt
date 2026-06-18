package com.roll24.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = Black,
    primaryContainer = MediumGray,
    onPrimaryContainer = White,
    secondary = AccentWarm,
    onSecondary = Black,
    secondaryContainer = MediumGray,
    onSecondaryContainer = White,
    tertiary = FilmCool,
    onTertiary = Black,
    tertiaryContainer = MediumGray,
    onTertiaryContainer = White,
    background = Black,
    onBackground = White,
    surface = DarkGray,
    onSurface = White,
    surfaceVariant = MediumGray,
    onSurfaceVariant = LightGray,
    outline = LightGray,
    outlineVariant = MediumGray
)

private val Roll24Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(Roll24Radius.Sm),
    medium = RoundedCornerShape(Roll24Radius.Md),
    large = RoundedCornerShape(Roll24Radius.Lg),
    extraLarge = RoundedCornerShape(Roll24Radius.Xl)
)

@Composable
fun Roll24Theme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Black.toArgb()
            window.navigationBarColor = Black.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Roll24Shapes,
        content = content
    )
}
