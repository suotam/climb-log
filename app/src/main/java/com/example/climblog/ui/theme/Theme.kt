package com.example.climblog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = StoneOrange,
    onPrimary = WarmWhite,
    primaryContainer = StoneOrangeContainer,
    onPrimaryContainer = StoneOrange,
    secondary = SlateGrey,
    onSecondary = WarmWhite,
    secondaryContainer = SlateGreyContainer,
    onSecondaryContainer = SlateGrey,
    tertiary = ForestGreen,
    onTertiary = WarmWhite,
    tertiaryContainer = ForestGreenContainer,
    onTertiaryContainer = ForestGreen,
    background = WarmWhite,
    onBackground = SlateGrey,
    surface = WarmWhite,
    onSurface = SlateGrey,
    surfaceVariant = SlateGreyContainer,
    onSurfaceVariant = SlateGrey
)

private val DarkColorScheme = darkColorScheme(
    primary = StoneOrangeDark,
    onPrimary = StoneOrangeContainerDark,
    primaryContainer = StoneOrangeContainerDark,
    onPrimaryContainer = StoneOrangeDark,
    secondary = SlateGreyDark,
    onSecondary = SlateGreyContainerDark,
    secondaryContainer = SlateGreyContainerDark,
    onSecondaryContainer = SlateGreyDark,
    tertiary = ForestGreenDark,
    onTertiary = ForestGreenContainerDark,
    tertiaryContainer = ForestGreenContainerDark,
    onTertiaryContainer = ForestGreenDark,
    background = DarkBackground,
    onBackground = SlateGreyContainer,
    surface = DarkSurface,
    onSurface = SlateGreyContainer,
    surfaceVariant = SlateGreyContainerDark,
    onSurfaceVariant = SlateGreyDark
)

@Composable
fun ClimbLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic color záměrně vypnuto — chceme konzistentní lezeckou paletu
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
