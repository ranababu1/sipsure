package app.sipsure.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The app is deliberately one immersive dark "ocean" look; every screen sits on the same gradient. */
object Ocean {
    val BgTop = Color(0xFF04121C)
    val BgMid = Color(0xFF082B40)
    val BgBottom = Color(0xFF0B5A75)

    val Aqua = Color(0xFF4FD1F5)
    val AquaBright = Color(0xFF2FB8E8)
    val AquaDeep = Color(0xFF1479C4)
    val Foam = Color(0xFFD5F6FF)
    val Amber = Color(0xFFFFB347)
    val Coral = Color(0xFFFF7B6B)
    val Mint = Color(0xFF5BE3B0)

    val TextHigh = Color(0xFFF2FAFD)
    val TextMid = Color(0xFFB4CFDB)
    val TextLow = Color(0xFF7FA0B0)

    val Glass = Color.White.copy(alpha = 0.08f)
    val GlassStrong = Color.White.copy(alpha = 0.14f)
    val GlassBorder = Color.White.copy(alpha = 0.14f)

    // Water colours per state, deep enough that white numerals stay readable on top.
    val WaterCalm = Color(0xFF1E9FD8)
    val WaterDue = Color(0xFFD9822B)
    val WaterMissed = Color(0xFFCF5547)
    val WaterBypass = Color(0xFF27AE86)
    val WaterPaused = Color(0xFF6A8BA5)
}

private val Scheme = darkColorScheme(
    primary = Ocean.Aqua,
    onPrimary = Color(0xFF00232F),
    primaryContainer = Color(0xFF0E4A63),
    onPrimaryContainer = Ocean.Foam,
    secondary = Color(0xFF9CCBDD),
    onSecondary = Color(0xFF0A2B3A),
    secondaryContainer = Color(0xFF1B4559),
    onSecondaryContainer = Ocean.Foam,
    tertiary = Ocean.Coral,
    onTertiary = Color(0xFF4A0F06),
    tertiaryContainer = Color(0xFF6E2A1E),
    onTertiaryContainer = Color(0xFFFFDAD3),
    background = Ocean.BgTop,
    onBackground = Ocean.TextHigh,
    surface = Color(0xFF0E2D40),
    onSurface = Ocean.TextHigh,
    surfaceVariant = Color(0xFF173C52),
    onSurfaceVariant = Ocean.TextMid,
    outline = Ocean.TextLow,
    outlineVariant = Color(0xFF2B4B5E),
    error = Color(0xFFFFB4AB),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 68.sp, lineHeight = 72.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp),
    displayMedium = TextStyle(fontSize = 44.sp, lineHeight = 48.sp, fontWeight = FontWeight.Light, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun SipSureTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = AppTypography, shapes = AppShapes, content = content)
}
