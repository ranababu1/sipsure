package app.sipsure.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sipsure.R

/**
 * One calm, dark "midnight water" look: near-black navy surfaces, hairline borders, and colour used
 * only where it means something (aqua = on track, amber = due, coral = missed, mint = covered).
 */
object Ocean {
    val BgTop = Color(0xFF060A11)
    val BgMid = Color(0xFF08111C)
    val BgBottom = Color(0xFF0A1826)

    val Aqua = Color(0xFF5CDCFF)
    val AquaBright = Color(0xFF46C2FF)
    val AquaDeep = Color(0xFF3A7BFF)
    val Foam = Color(0xFFDDF7FF)
    val Amber = Color(0xFFFFB45C)
    val Coral = Color(0xFFFF6F6F)
    val Mint = Color(0xFF4FE0B4)

    val TextHigh = Color(0xFFF3F7FB)
    val TextMid = Color(0xFFA3B1C2)
    val TextLow = Color(0xFF6B7A8C)

    /** Solid raised surface (cards), slightly lighter than the page. */
    val Surface = Color(0xFF0E1723)
    val SurfaceHigh = Color(0xFF142030)
    val Hairline = Color.White.copy(alpha = 0.07f)

    val Glass = Color.White.copy(alpha = 0.05f)
    val GlassStrong = Color.White.copy(alpha = 0.10f)
    val GlassBorder = Color.White.copy(alpha = 0.09f)

    // Water colours per state, deep enough that white numerals stay readable on top.
    val WaterCalm = Color(0xFF2A9FE6)
    val WaterDue = Color(0xFFE08A34)
    val WaterMissed = Color(0xFFD9534F)
    val WaterBypass = Color(0xFF22B38A)
    val WaterPaused = Color(0xFF5E7389)

    val AquaGradient = Brush.linearGradient(listOf(Color(0xFF5CDCFF), Color(0xFF3A7BFF)))
    val AmberGradient = Brush.linearGradient(listOf(Color(0xFFFFC46B), Color(0xFFFF8A4C)))
}

@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: Int) = Font(
    R.font.manrope, FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Manrope (variable, bundled, SIL OFL; licence in assets/licenses) for a crisp, modern feel. */
val Manrope = FontFamily(manrope(300), manrope(400), manrope(500), manrope(600), manrope(700), manrope(800))

private val Scheme = darkColorScheme(
    primary = Ocean.Aqua,
    onPrimary = Color(0xFF00222E),
    primaryContainer = Color(0xFF0F3A52),
    onPrimaryContainer = Ocean.Foam,
    secondary = Color(0xFF9FB8CC),
    onSecondary = Color(0xFF0A1D2B),
    secondaryContainer = Color(0xFF1A2B3C),
    onSecondaryContainer = Ocean.Foam,
    tertiary = Ocean.Coral,
    onTertiary = Color(0xFF450A0A),
    background = Ocean.BgTop,
    onBackground = Ocean.TextHigh,
    surface = Ocean.Surface,
    onSurface = Ocean.TextHigh,
    surfaceVariant = Ocean.SurfaceHigh,
    onSurfaceVariant = Ocean.TextMid,
    surfaceContainer = Ocean.Surface,
    surfaceContainerHigh = Ocean.SurfaceHigh,
    surfaceContainerHighest = Color(0xFF1A2839),
    inverseSurface = Color(0xFFE6EEF6),
    inverseOnSurface = Color(0xFF0B1520),
    outline = Ocean.TextLow,
    outlineVariant = Color(0xFF223142),
    error = Color(0xFFFF8A80),
)

private fun style(size: Int, line: Int, weight: Int, spacing: Float = 0f) = TextStyle(
    fontFamily = Manrope, fontSize = size.sp, lineHeight = line.sp, fontWeight = FontWeight(weight), letterSpacing = spacing.sp,
)

private val AppTypography = Typography(
    displayLarge = style(76, 80, 300, -3f),
    displayMedium = style(44, 48, 400, -1.5f),
    displaySmall = style(34, 40, 500, -1f),
    headlineLarge = style(30, 36, 700, -0.8f),
    headlineMedium = style(24, 30, 700, -0.5f),
    headlineSmall = style(20, 26, 700, -0.3f),
    titleLarge = style(18, 24, 700, -0.2f),
    titleMedium = style(15, 20, 600),
    titleSmall = style(14, 18, 600),
    bodyLarge = style(16, 24, 500),
    bodyMedium = style(14, 20, 500),
    bodySmall = style(12, 16, 500),
    labelLarge = style(16, 20, 700),
    labelMedium = style(11, 14, 700, 1.2f),
    labelSmall = style(11, 14, 600, 0.3f),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun SipSureTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = AppTypography, shapes = AppShapes, content = content)
}
