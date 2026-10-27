package app.sipsure.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sipsure.ui.theme.Ocean
import kotlin.math.PI
import kotlin.math.sin

// ---------- Backdrop ----------

/** Full-screen ocean gradient with a few soft light blobs; every screen sits on this. */
@Composable
fun OceanBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ocean.BgTop, Ocean.BgMid, Ocean.BgBottom)))) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Ocean.Aqua.copy(alpha = 0.10f), size.width * 0.7f, Offset(size.width * 1.0f, size.height * 0.12f))
            drawCircle(Ocean.AquaDeep.copy(alpha = 0.14f), size.width * 0.8f, Offset(-size.width * 0.1f, size.height * 0.85f))
        }
        content()
    }
}

// ---------- The water glass ----------

/**
 * The centrepiece: a glass that is filled with animated, layered waves and rising bubbles.
 * [level] is 0..1 and animates smoothly; [content] is drawn centred on top.
 */
@Composable
fun WaterGlass(
    level: Float,
    water: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 300.dp,
    description: String,
    content: @Composable BoxScope.() -> Unit,
) {
    val animatedLevel by animateFloatAsState(level.coerceIn(0f, 1f), spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessVeryLow), label = "level")
    val transition = rememberInfiniteTransition(label = "water")
    val phaseA by transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "a")
    val phaseB by transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(6800, easing = LinearEasing)), label = "b")
    val bubbles by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "bubbles")

    Box(modifier.size(diameter).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2
            val circle = Path().apply { addOval(Rect(center, radius)) }
            drawCircle(Color.White.copy(alpha = 0.06f), radius)
            clipPath(circle) {
                val top = size.height * (1f - animatedLevel)
                val amplitude = 9.dp.toPx() * (1f - (animatedLevel - 0.5f) * (animatedLevel - 0.5f) * 2.2f).coerceIn(0.3f, 1f)

                fun wave(phase: Float, amp: Float, frequency: Float, color: Color) {
                    val path = Path().apply {
                        moveTo(0f, size.height)
                        var x = 0f
                        while (x <= size.width + 4f) {
                            lineTo(x, top + sin(x / size.width * 2f * PI.toFloat() * frequency + phase) * amp)
                            x += 4f
                        }
                        lineTo(size.width, size.height)
                        close()
                    }
                    drawPath(path, color)
                }
                wave(phaseB, amplitude * 1.1f, 1.1f, water.copy(alpha = 0.45f))
                wave(phaseA, amplitude, 1.6f, water.copy(alpha = 0.9f))

                // A few bubbles drifting up through the water.
                for (i in 0 until 6) {
                    val progress = (bubbles + i / 6f) % 1f
                    val x = size.width * (0.2f + 0.12f * i) + sin(progress * 2f * PI.toFloat() + i) * 10.dp.toPx()
                    val y = size.height - progress * (size.height - top)
                    if (y > top + 10.dp.toPx()) {
                        drawCircle(Color.White.copy(alpha = 0.28f * (1f - progress)), (3 + i % 3).dp.toPx(), Offset(x, y))
                    }
                }
            }
            // Glass rim and a soft highlight.
            drawCircle(Color.White.copy(alpha = 0.28f), radius - 1.5.dp.toPx(), style = Stroke(3.dp.toPx()))
            drawArc(
                Color.White.copy(alpha = 0.22f), 200f, 50f, false,
                Offset(size.width * 0.1f, size.height * 0.1f), Size(size.width * 0.8f, size.height * 0.8f),
                style = Stroke(5.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        content()
    }
}

// ---------- Surfaces ----------

@Composable
fun GlassCard(modifier: Modifier = Modifier, padding: Dp = 18.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Ocean.Glass)
            .border(BorderStroke(1.dp, Ocean.GlassBorder), RoundedCornerShape(26.dp))
            .padding(padding),
        content = content,
    )
}

/** Gives any clickable a springy press-in, the way expressive Material components feel. */
fun Modifier.springClick(enabled: Boolean = true, role: Role = Role.Button, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), label = "press")
    this.scale(scale).clickable(source, null, enabled, role = role, onClick = onClick)
}

enum class ButtonKind { Primary, Amber, Glass, Danger }

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
        ButtonKind.Primary -> Triple(Brush.horizontalGradient(listOf(Ocean.AquaBright, Ocean.AquaDeep)), null, Color.White)
        ButtonKind.Amber -> Triple(Brush.horizontalGradient(listOf(Color(0xFFFFB347), Color(0xFFF08A24))), null, Color(0xFF2A1700))
        ButtonKind.Glass -> Triple(Brush.linearGradient(listOf(Ocean.Glass, Ocean.Glass)), Ocean.GlassBorder, Ocean.TextHigh)
        ButtonKind.Danger -> Triple(Brush.linearGradient(listOf(Ocean.Coral.copy(alpha = 0.14f), Ocean.Coral.copy(alpha = 0.14f))), Ocean.Coral.copy(alpha = 0.5f), Ocean.Coral)
    }
    Row(
        modifier.fillMaxWidth().height(62.dp).clip(shape).background(fill)
            .let { if (border != null) it.border(1.dp, border, shape) else it }
            .springClick(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) {
            glyph(ink)
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp), color = ink)
    }
}

@Composable
fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier.clip(shape)
            .background(if (selected) Brush.horizontalGradient(listOf(Ocean.AquaBright, Ocean.AquaDeep)) else Brush.linearGradient(listOf(Ocean.Glass, Ocean.Glass)))
            .border(1.dp, if (selected) Color.Transparent else Ocean.GlassBorder, shape)
            .springClick(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, style = MaterialTheme.typography.titleMedium,
            color = when {
                !enabled -> Ocean.TextLow.copy(alpha = 0.5f)
                selected -> Color.White
                else -> Ocean.TextHigh
            },
        )
    }
}

/** Small square tile with a glyph and a label, used for quick actions. */
@Composable
fun ActionTile(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Ocean.Aqua,
    detail: String? = null,
    glyph: @Composable (Color) -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier.clip(shape).background(Ocean.Glass).border(1.dp, Ocean.GlassBorder, shape)
            .springClick(onClick = onClick).padding(vertical = 16.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { glyph(accent) }
        Text(label, style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp), color = Ocean.TextHigh)
        if (detail != null) Text(detail, style = MaterialTheme.typography.bodyMedium, color = Ocean.TextMid)
    }
}

@Composable
fun StatusPill(text: String, tint: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = 0.16f)).padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(tint))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelMedium, color = Ocean.TextLow)
}

// ---------- Floating navigation ----------

data class NavItem(val label: String, val glyph: @Composable (Color) -> Unit)

@Composable
fun FloatingNav(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.navigationBarsPadding().padding(bottom = 12.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF0A2638).copy(alpha = 0.92f))
            .border(1.dp, Ocean.GlassBorder, RoundedCornerShape(50))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEachIndexed { index, item ->
            val active = index == selected
            val ink = if (active) Color.White else Ocean.TextMid
            Row(
                Modifier.clip(RoundedCornerShape(50))
                    .background(if (active) Brush.horizontalGradient(listOf(Ocean.AquaBright, Ocean.AquaDeep)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                    .springClick(role = Role.Tab) { onSelect(index) }
                    .semantics { contentDescription = item.label }
                    .padding(horizontal = if (active) 20.dp else 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item.glyph(ink)
                if (active) {
                    Spacer(Modifier.width(8.dp))
                    Text(item.label, style = MaterialTheme.typography.titleMedium, color = ink)
                }
            }
        }
    }
}

// ---------- Hand-drawn glyphs (no icon library needed) ----------

@Composable
fun CameraGlyph(color: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.09f
        drawRoundRect(color, Offset(w * 0.06f, h * 0.24f), Size(w * 0.88f, h * 0.62f), androidx.compose.ui.geometry.CornerRadius(w * 0.14f), style = Stroke(stroke))
        drawRoundRect(color, Offset(w * 0.32f, h * 0.12f), Size(w * 0.36f, h * 0.16f), androidx.compose.ui.geometry.CornerRadius(w * 0.05f))
        drawCircle(color, w * 0.19f, Offset(w * 0.5f, h * 0.55f), style = Stroke(stroke))
    }
}

@Composable
fun PauseGlyph(color: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawRoundRect(color, Offset(w * 0.22f, h * 0.18f), Size(w * 0.2f, h * 0.64f), androidx.compose.ui.geometry.CornerRadius(w * 0.06f))
        drawRoundRect(color, Offset(w * 0.58f, h * 0.18f), Size(w * 0.2f, h * 0.64f), androidx.compose.ui.geometry.CornerRadius(w * 0.06f))
    }
}

@Composable
fun PlayGlyph(color: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawPath(
            Path().apply { moveTo(w * 0.28f, h * 0.16f); lineTo(w * 0.84f, h * 0.5f); lineTo(w * 0.28f, h * 0.84f); close() },
            color,
        )
    }
}

@Composable
fun CoinGlyph(color: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        drawCircle(color, w * 0.44f, center, style = Stroke(w * 0.09f))
        drawCircle(color, w * 0.2f, center)
    }
}

@Composable
fun SosGlyph(color: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawPath(
            Path().apply { moveTo(w * 0.5f, h * 0.08f); lineTo(w * 0.94f, h * 0.88f); lineTo(w * 0.06f, h * 0.88f); close() },
            color, style = Stroke(w * 0.09f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
        )
        drawLine(color, Offset(w * 0.5f, h * 0.38f), Offset(w * 0.5f, h * 0.62f), w * 0.09f, StrokeCap.Round)
        drawCircle(color, w * 0.05f, Offset(w * 0.5f, h * 0.74f))
    }
}

@Composable
fun LockGlyph(color: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawRoundRect(color, Offset(w * 0.18f, h * 0.44f), Size(w * 0.64f, h * 0.46f), androidx.compose.ui.geometry.CornerRadius(w * 0.1f))
        drawArc(color, 180f, 180f, false, Offset(w * 0.3f, h * 0.12f), Size(w * 0.4f, h * 0.7f), style = Stroke(w * 0.09f, cap = StrokeCap.Round))
    }
}

@Composable
fun GearGlyph(color: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        for (i in 0 until 8) {
            val angle = i * PI.toFloat() / 4f
            val dx = kotlin.math.cos(angle)
            val dy = sin(angle)
            drawLine(color, Offset(center.x + dx * w * 0.34f, center.y + dy * w * 0.34f), Offset(center.x + dx * w * 0.46f, center.y + dy * w * 0.46f), w * 0.1f, StrokeCap.Round)
        }
        drawCircle(color, w * 0.28f, center, style = Stroke(w * 0.1f))
    }
}

@Composable
fun CheckGlyph(color: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawPath(
            Path().apply { moveTo(w * 0.2f, h * 0.54f); lineTo(w * 0.42f, h * 0.74f); lineTo(w * 0.8f, h * 0.28f) },
            color, style = Stroke(w * 0.11f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
        )
    }
}

@Composable
fun DropGlyph(color: Color, size: Dp = 24.dp) {
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
fun PhoneGlyph(color: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        drawArc(color, 110f, 140f, false, Offset(w * 0.14f, w * 0.14f), Size(w * 0.72f, w * 0.72f), style = Stroke(w * 0.2f, cap = StrokeCap.Round))
    }
}
