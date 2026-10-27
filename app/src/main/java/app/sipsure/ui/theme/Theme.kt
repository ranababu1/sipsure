package app.sipsure.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

private val Ocean = Color(0xFF0A6B94)
private val Aqua = Color(0xFF12A4C9)

private val Light = lightColorScheme(
    primary = Ocean,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3EEF9),
    onPrimaryContainer = Color(0xFF00384D),
    secondary = Color(0xFF3E6577),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDEBF2),
    onSecondaryContainer = Color(0xFF142C38),
    tertiary = Color(0xFFB4492E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBD1),
    onTertiaryContainer = Color(0xFF4A1204),
    background = Color(0xFFF3F9FC),
    onBackground = Color(0xFF0F1D24),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F1D24),
    surfaceVariant = Color(0xFFE6EFF3),
    onSurfaceVariant = Color(0xFF475A64),
    outline = Color(0xFF8499A4),
    outlineVariant = Color(0xFFCADAE1),
    error = Color(0xFFB3261E),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF58CBF0),
    onPrimary = Color(0xFF00364A),
    primaryContainer = Color(0xFF0C4A63),
    onPrimaryContainer = Color(0xFFCDEEFB),
    secondary = Color(0xFFA8CBDB),
    onSecondary = Color(0xFF123340),
    secondaryContainer = Color(0xFF2A4857),
    onSecondaryContainer = Color(0xFFDDEBF2),
    tertiary = Color(0xFFFFB5A0),
    onTertiary = Color(0xFF5A1D0B),
    tertiaryContainer = Color(0xFF7B2F18),
    onTertiaryContainer = Color(0xFFFFDBD1),
    background = Color(0xFF071820),
    onBackground = Color(0xFFDCEAF0),
    surface = Color(0xFF0D2530),
    onSurface = Color(0xFFDCEAF0),
    surfaceVariant = Color(0xFF1B3441),
    onSurfaceVariant = Color(0xFFA9C0CB),
    outline = Color(0xFF6F8A97),
    outlineVariant = Color(0xFF2B4857),
    error = Color(0xFFFFB4AB),
)

/** Status colours for hero cards; white text sits on these in both themes. */
object Hero {
    val Calm = listOf(Color(0xFF0A5A86), Color(0xFF0E94B8))
    val Due = listOf(Color(0xFF8A4306), Color(0xFFBE6309))
    val Missed = listOf(Color(0xFF8E2A2A), Color(0xFFBD403C))
    val Bypass = listOf(Color(0xFF17604A), Color(0xFF28896A))
    val Paused = listOf(Color(0xFF3D4F60), Color(0xFF5A6E84))
}

private val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 56.sp, lineHeight = 60.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
)

val AccentAqua = Aqua

@Composable
fun SipSureTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) Dark else Light,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
