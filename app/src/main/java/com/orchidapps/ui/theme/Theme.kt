package com.orchidapps.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orchidapps.R

/** Palet dari desain hi-fi Orchid Apps (Figma). */
object OrchidColors {
    val Forest = Color(0xFF324A3A)
    val ForestDark = Color(0xFF263B2E)
    val Blush = Color(0xFFF6C2CF)
    val BlushLight = Color(0xFFFBE3EA)
    val Cream = Color(0xFFF4F0E4)
    val Paper = Color(0xFFFEFCF7)
    val MapGreen = Color(0xFFE5E9DA)
    val HillLight = Color(0xFFAABEA7)
    val HillMid = Color(0xFF5B8166)
    val Lake = Color(0xFFC6D9CF)
    val Muted = Color(0xFF6B7A6E)
    val Danger = Color(0xFFB3261E)
}

private val ColorScheme = lightColorScheme(
    primary = OrchidColors.Forest,
    onPrimary = Color.White,
    primaryContainer = OrchidColors.Forest,
    onPrimaryContainer = Color.White,
    secondary = OrchidColors.HillMid,
    onSecondary = Color.White,
    secondaryContainer = OrchidColors.Blush,
    onSecondaryContainer = OrchidColors.ForestDark,
    tertiary = Color(0xFF9A4A62),
    onTertiary = Color.White,
    tertiaryContainer = OrchidColors.BlushLight,
    onTertiaryContainer = OrchidColors.ForestDark,
    background = OrchidColors.Cream,
    onBackground = OrchidColors.ForestDark,
    surface = OrchidColors.Cream,
    onSurface = OrchidColors.ForestDark,
    surfaceVariant = OrchidColors.Paper,
    onSurfaceVariant = OrchidColors.Muted,
    surfaceContainer = OrchidColors.Paper,
    surfaceContainerHigh = OrchidColors.Paper,
    surfaceContainerLow = OrchidColors.Paper,
    outline = OrchidColors.Forest,
    outlineVariant = Color(0xFFD9D6C8),
    error = OrchidColors.Danger,
    errorContainer = OrchidColors.BlushLight,
    onErrorContainer = OrchidColors.ForestDark,
)

/**
 * Font dari desain Figma: Fraunces untuk judul dan Plus Jakarta Sans untuk teks isi.
 * Keduanya font variabel berlisensi SIL OFL (lihat assets/licenses), disertakan di aplikasi
 * agar tetap tampil tanpa internet.
 */
@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: FontWeight) = Font(
    resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val Display = FontFamily(
    variable(R.font.fraunces, FontWeight.SemiBold),
    variable(R.font.fraunces, FontWeight.Bold),
)

private val Body = FontFamily(
    variable(R.font.plus_jakarta_sans, FontWeight.Normal),
    variable(R.font.plus_jakarta_sans, FontWeight.Medium),
    variable(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    variable(R.font.plus_jakarta_sans, FontWeight.Bold),
)

private val OrchidTypography = Typography().run {
    fun TextStyle.body(weight: FontWeight? = null) = copy(fontFamily = Body, fontWeight = weight ?: fontWeight)
    fun TextStyle.display() = copy(fontFamily = Display, fontWeight = FontWeight.SemiBold)
    copy(
        displayLarge = displayLarge.display(),
        displayMedium = displayMedium.display(),
        displaySmall = displaySmall.display(),
        headlineLarge = headlineLarge.display(),
        headlineMedium = headlineMedium.display(),
        headlineSmall = headlineSmall.display(),
        titleLarge = titleLarge.display(),
        titleMedium = titleMedium.body(FontWeight.SemiBold),
        titleSmall = titleSmall.body(FontWeight.SemiBold),
        bodyLarge = bodyLarge.body(),
        bodyMedium = bodyMedium.body(),
        bodySmall = bodySmall.body(),
        labelLarge = labelLarge.body(FontWeight.SemiBold),
        labelMedium = labelMedium.body(FontWeight.Medium),
        labelSmall = TextStyle(fontFamily = Body, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    )
}

private val OrchidShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun OrchidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = OrchidTypography,
        shapes = OrchidShapes,
        content = content,
    )
}
