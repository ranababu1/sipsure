package app.sipsure.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sipsure.ui.theme.Ocean
import kotlin.math.PI
import kotlin.math.sin

// ---------- Surfaces ----------

private val CardShape = RoundedCornerShape(24.dp)

/** A raised card: solid, slightly lighter than the page, with a hairline edge that catches light at the top. */
@Composable
fun GlassCard(modifier: Modifier = Modifier, padding: Dp = 20.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth()
            .clip(CardShape)
            .background(Brush.verticalGradient(listOf(Ocean.SurfaceHigh, Ocean.Surface)))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.03f))), CardShape)
            .padding(padding),
        content = content,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Ocean.Hairline))
}

/** Gives any clickable a soft spring press-in. */
fun Modifier.springClick(enabled: Boolean = true, role: Role = Role.Button, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), label = "press")
    this.scale(scale).clickable(source, null, enabled, role = role, onClick = onClick)
}

// ---------- Buttons ----------

enum class ButtonKind { Primary, Amber, Glass, Danger }

private val DarkInk = Color(0xFF041A26)

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    glyph: (@Composable (Color) -> Unit)? = null,
) {
    val shape = RoundedCornerShape(50)
    val (fill, border, ink) = when (kind) {
        ButtonKind.Primary -> Triple(Ocean.AquaGradient, null, DarkInk)
        ButtonKind.Amber -> Triple(Ocean.AmberGradient, null, Color(0xFF2A1400))
        ButtonKind.Glass -> Triple(SolidColor(Ocean.Glass), Ocean.GlassBorder, Ocean.TextHigh)
        ButtonKind.Danger -> Triple(SolidColor(Ocean.Coral.copy(alpha = 0.10f)), Ocean.Coral.copy(alpha = 0.35f), Ocean.Coral)
    }
    Row(
        modifier.fillMaxWidth().height(58.dp).clip(shape).background(fill)
            .let { if (border != null) it.border(1.dp, border, shape) else it }
            .springClick(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) {
            glyph(ink)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = ink)
    }
}

/** A compact secondary action: icon over a short label, for rows of two or three. */
@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, glyph: @Composable (Color) -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier.height(52.dp).clip(shape).background(Ocean.Glass).border(1.dp, Ocean.GlassBorder, shape)
            .springClick(onClick = onClick).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        glyph(Ocean.TextMid)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, color = Ocean.TextHigh)
    }
}

@Composable
fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val shape = RoundedCornerShape(50)
    val bg by animateColorAsState(if (selected) Ocean.Aqua else Ocean.Glass, tween(220), label = "chip")
    Box(
        modifier.clip(shape).background(bg)
            .border(1.dp, if (selected) Color.Transparent else Ocean.GlassBorder, shape)
            .springClick(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, style = MaterialTheme.typography.titleSmall,
            color = when {
                !enabled -> Ocean.TextLow.copy(alpha = 0.5f)
                selected -> DarkInk
                else -> Ocean.TextHigh
            },
        )
    }
}

// ---------- Small pieces ----------

/** A status badge with a gently pulsing dot. */
@Composable
fun StatusPill(text: String, tint: Color, modifier: Modifier = Modifier, pulse: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "pill")
    val halo by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "halo")
    Row(
        modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.22f), RoundedCornerShape(50))
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(14.dp)) {
            if (pulse) drawCircle(tint.copy(alpha = 0.35f * (1f - halo)), size.minDimension / 2 * (0.5f + 0.5f * halo))
            drawCircle(tint, size.minDimension * 0.2f)
        }
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier.padding(start = 4.dp, top = 8.dp), style = MaterialTheme.typography.labelMedium, color = Ocean.TextLow)
}

/** A round tinted badge holding a glyph. */
@Composable
fun GlyphBadge(tint: Color, size: Dp = 40.dp, glyph: @Composable (Color) -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) { glyph(tint) }
}

/** One row of a list: badge, title and subtitle, optional trailing content. */
@Composable
fun ListRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ocean.TextHigh)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Ocean.TextLow)
        }
        if (trailing != null) trailing()
    }
}

data class Stat(val label: String, val value: String, val tint: Color = Ocean.TextHigh)

/** Two to four figures side by side, separated by hairlines. */
@Composable
fun StatStrip(stats: List<Stat>, modifier: Modifier = Modifier) {
    GlassCard(modifier, padding = 0.dp) {
        Row(Modifier.fillMaxWidth().height(84.dp), verticalAlignment = Alignment.CenterVertically) {
            stats.forEachIndexed { index, stat ->
                if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().padding(vertical = 20.dp).background(Ocean.Hairline))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stat.value, style = MaterialTheme.typography.headlineSmall, color = stat.tint, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(2.dp))
                    Text(stat.label, style = MaterialTheme.typography.bodySmall, color = Ocean.TextLow, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

// ---------- Bottom navigation ----------

data class NavItem(val label: String, val glyph: @Composable (Color) -> Unit)

/** A slim, frosted bar pinned to the bottom edge with a soft indicator behind the active icon. */
@Composable
fun BottomBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(Ocean.BgTop.copy(alpha = 0.94f))) {
        Hairline()
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { index, item ->
                val active = index == selected
                val ink by animateColorAsState(if (active) Ocean.Aqua else Ocean.TextLow, tween(220), label = "ink")
                val pill by animateColorAsState(if (active) Ocean.Aqua.copy(alpha = 0.14f) else Color.Transparent, tween(220), label = "pill")
                Column(
                    Modifier.weight(1f).fillMaxHeight()
                        .clickable(remember { MutableInteractionSource() }, null, role = Role.Tab) { onSelect(index) }
                        .semantics { contentDescription = item.label; this.selected = active },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(Modifier.width(60.dp).height(32.dp).clip(RoundedCornerShape(50)).background(pill), contentAlignment = Alignment.Center) {
                        item.glyph(ink)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(item.label, style = MaterialTheme.typography.labelSmall, color = ink)
                }
            }
        }
    }
}

// ---------- Hand-drawn glyphs (no icon library needed) ----------

@Composable
fun CameraGlyph(color: Color, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.085f
        drawRoundRect(color, Offset(w * 0.08f, h * 0.26f), Size(w * 0.84f, h * 0.6f), androidx.compose.ui.geometry.CornerRadius(w * 0.16f), style = Stroke(stroke))
        drawRoundRect(color, Offset(w * 0.34f, h * 0.14f), Size(w * 0.32f, h * 0.14f), androidx.compose.ui.geometry.CornerRadius(w * 0.05f))
        drawCircle(color, w * 0.17f, Offset(w * 0.5f, h * 0.56f), style = Stroke(stroke))
    }
}

@Composable
fun PauseGlyph(color: Color, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawLine(color, Offset(w * 0.34f, h * 0.24f), Offset(w * 0.34f, h * 0.76f), w * 0.13f, StrokeCap.Round)
        drawLine(color, Offset(w * 0.66f, h * 0.24f), Offset(w * 0.66f, h * 0.76f), w * 0.13f, StrokeCap.Round)
    }
}

@Composable
fun PlayGlyph(color: Color, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawPath(
            Path().apply { moveTo(w * 0.3f, h * 0.18f); lineTo(w * 0.82f, h * 0.5f); lineTo(w * 0.3f, h * 0.82f); close() },
            color, style = Stroke(w * 0.1f, join = androidx.compose.ui.graphics.StrokeJoin.Round),
        )
        drawPath(Path().apply { moveTo(w * 0.3f, h * 0.18f); lineTo(w * 0.82f, h * 0.5f); lineTo(w * 0.3f, h * 0.82f); close() }, color)
    }
}

@Composable
fun CoinGlyph(color: Color, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        drawCircle(color, w * 0.4f, center, style = Stroke(w * 0.085f))
        drawCircle(color, w * 0.22f, center, style = Stroke(w * 0.085f))
        drawCircle(color, w * 0.07f, center)
    }
}

@Composable
fun SosGlyph(color: Color, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawPath(
            Path().apply { moveTo(w * 0.5f, h * 0.1f); lineTo(w * 0.92f, h * 0.86f); lineTo(w * 0.08f, h * 0.86f); close() },
            color, style = Stroke(w * 0.085f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
        )
        drawLine(color, Offset(w * 0.5f, h * 0.4f), Offset(w * 0.5f, h * 0.6f), w * 0.085f, StrokeCap.Round)
        drawCircle(color, w * 0.05f, Offset(w * 0.5f, h * 0.72f))
    }
}

@Composable
fun LockGlyph(color: Color, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawRoundRect(color, Offset(w * 0.18f, h * 0.44f), Size(w * 0.64f, h * 0.46f), androidx.compose.ui.geometry.CornerRadius(w * 0.12f), style = Stroke(w * 0.085f))
        drawArc(color, 180f, 180f, false, Offset(w * 0.3f, h * 0.12f), Size(w * 0.4f, h * 0.64f), style = Stroke(w * 0.085f, cap = StrokeCap.Round))
        drawCircle(color, w * 0.06f, Offset(w * 0.5f, h * 0.66f))
    }
}

@Composable
fun GearGlyph(color: Color, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        for (i in 0 until 8) {
            val angle = i * PI.toFloat() / 4f
            val dx = kotlin.math.cos(angle)
            val dy = sin(angle)
            drawLine(color, Offset(center.x + dx * w * 0.32f, center.y + dy * w * 0.32f), Offset(center.x + dx * w * 0.44f, center.y + dy * w * 0.44f), w * 0.11f, StrokeCap.Round)
        }
        drawCircle(color, w * 0.28f, center, style = Stroke(w * 0.085f))
        drawCircle(color, w * 0.1f, center, style = Stroke(w * 0.085f))
    }
}

@Composable
fun CheckGlyph(color: Color, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawPath(
            Path().apply { moveTo(w * 0.22f, h * 0.52f); lineTo(w * 0.42f, h * 0.72f); lineTo(w * 0.8f, h * 0.3f) },
            color, style = Stroke(w * 0.1f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
        )
    }
}

@Composable
fun BackGlyph(color: Color, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawPath(
            Path().apply { moveTo(w * 0.62f, h * 0.2f); lineTo(w * 0.32f, h * 0.5f); lineTo(w * 0.62f, h * 0.8f) },
            color, style = Stroke(w * 0.11f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
        )
    }
}

@Composable
fun DropGlyph(color: Color, size: Dp = 22.dp) {
    WaterDrop(Modifier.size(width = size * 0.72f, height = size), color)
}

/** A teardrop, the app's one illustration motif. */
@Composable
fun WaterDrop(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawPath(
            Path().apply {
                moveTo(w / 2, 0f)
                cubicTo(w * 0.98f, h * 0.48f, w * 0.88f, h, w / 2, h)
                cubicTo(w * 0.12f, h, w * 0.02f, h * 0.48f, w / 2, 0f)
                close()
            },
            color,
        )
    }
}

@Composable
fun CoreIcon(icon: ImageVector, tint: Color, size: Dp = 22.dp) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size))
}

@Composable
fun PhoneGlyph(color: Color, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // Handset: two rounded ends joined by a curved grip.
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.14f)
            quadraticTo(w * 0.08f, h * 0.24f, w * 0.14f, h * 0.38f)
            cubicTo(w * 0.28f, h * 0.68f, w * 0.42f, h * 0.8f, w * 0.64f, h * 0.88f)
            quadraticTo(w * 0.78f, h * 0.92f, w * 0.86f, h * 0.8f)
        }
        drawPath(path, color, style = Stroke(w * 0.13f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}
