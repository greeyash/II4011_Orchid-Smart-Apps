package com.orchidapps.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PineGreen = Color(0xFF2E5E3E)
private val PineGreenLight = Color(0xFF8BC9A0)
private val Orchid = Color(0xFF8E4FA3)
private val OrchidLight = Color(0xFFE8C6F0)
private val WarningAmber = Color(0xFFB26A00)

private val LightColors = lightColorScheme(
    primary = PineGreen,
    onPrimary = Color.White,
    secondary = Orchid,
    onSecondary = Color.White,
    secondaryContainer = OrchidLight,
    tertiary = WarningAmber,
)

private val DarkColors = darkColorScheme(
    primary = PineGreenLight,
    secondary = OrchidLight,
    tertiary = Color(0xFFFFB74D),
)

@Composable
fun OrchidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
