package app.sipsure.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sipsure.ui.theme.Ocean
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.launch

private const val TAU = (2 * PI).toFloat()

/** Cheap deterministic 0..1 value per index, so bubbles look random but don't jump between frames. */
private fun hash01(i: Int, salt: Int): Float {
    var h = i * 374761393 + salt * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    return ((h xor (h ushr 16)) and 0xFFFF) / 65535f
}

/** Draws a wave band whose top edge follows a sine curve; returns the surface path for foam strokes. */
private fun DrawScope.wave(top: Float, amplitude: Float, frequency: Float, phase: Float, tilt: Float, color: Color, foam: Boolean = false) {
    val body = Path()
    val line = Path()
    var x = 0f
    var first = true
    while (x <= size.width + 4f) {
        val y = top + sin(x / size.width * TAU * frequency + phase) * amplitude + tilt * (x / size.width - 0.5f)
        if (first) {
            body.moveTo(x, y); line.moveTo(x, y); first = false
        } else {
            body.lineTo(x, y); line.lineTo(x, y)
        }
        x += 4f
    }
    body.lineTo(size.width, size.height)
    body.lineTo(0f, size.height)
    body.close()
    drawPath(body, color)
    if (foam) drawPath(line, Color.White.copy(alpha = 0.38f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
}

// ---------- Full-screen backdrop ----------

/** The gradient every screen sits on, with slow bubbles drifting up and rolling waves at the bottom edge. */
@Composable
fun OceanBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val transition = rememberInfiniteTransition(label = "ocean")
    val phaseA by transition.animateFloat(0f, TAU, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "waveA")
    val phaseB by transition.animateFloat(TAU, 0f, infiniteRepeatable(tween(13000, easing = LinearEasing)), label = "waveB")
    val drift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(26000, easing = LinearEasing)), label = "drift")
    val shimmer by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Reverse), label = "shimmer")

    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ocean.BgTop, Ocean.BgMid, Ocean.BgBottom)))) {
        Canvas(Modifier.fillMaxSize()) {
            // Slow pools of light.
            drawCircle(Ocean.Aqua.copy(alpha = 0.07f + 0.05f * shimmer), size.width * 0.7f, Offset(size.width * 1.0f, size.height * 0.12f))
            drawCircle(Ocean.AquaDeep.copy(alpha = 0.10f + 0.06f * (1f - shimmer)), size.width * 0.8f, Offset(-size.width * 0.1f, size.height * 0.85f))

            // Bubbles rising through the whole screen.
            for (i in 0 until 16) {
                val speed = 0.6f + hash01(i, 1) * 0.9f
                val progress = (drift * speed + hash01(i, 2)) % 1f
                val x = size.width * hash01(i, 3) + sin(progress * TAU * 2f + i) * 14.dp.toPx()
                val y = size.height * (1.05f - progress * 1.15f)
                val r = (2f + hash01(i, 4) * 6f).dp.toPx()
                val fade = (1f - abs(progress - 0.5f) * 2f).coerceIn(0f, 1f)
                drawCircle(Color.White.copy(alpha = 0.10f * fade), r, Offset(x, y))
                drawCircle(Color.White.copy(alpha = 0.18f * fade), r, Offset(x, y), style = Stroke(1.dp.toPx()))
            }

            // Rolling waves along the bottom.
            val base = size.height * 0.90f
            clipPath(Path().apply { addRect(Rect(0f, 0f, size.width, size.height)) }) {
                wave(base, 10.dp.toPx(), 1.2f, phaseB, 0f, Ocean.Aqua.copy(alpha = 0.07f))
                wave(base + 14.dp.toPx(), 12.dp.toPx(), 0.9f, phaseA, 0f, Ocean.AquaBright.copy(alpha = 0.09f))
                wave(base + 30.dp.toPx(), 9.dp.toPx(), 1.6f, phaseB * 1.3f, 0f, Ocean.AquaDeep.copy(alpha = 0.14f), foam = true)
            }
        }
        content()
    }
}

// ---------- The water glass ----------

/**
 * The centrepiece: a glass of living water. Three wave layers roll at different speeds, the surface
 * sloshes and tilts, drips fall in and ripple the surface, light dapples the depths and bubbles rise.
 * Tapping splashes it. [level] is 0..1 and animates with a spring; [content] is drawn centred on top.
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
    val animatedLevel by animateFloatAsState(level.coerceIn(0f, 1f), spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessVeryLow), label = "level")
    val transition = rememberInfiniteTransition(label = "water")
    val phaseA by transition.animateFloat(0f, TAU, infiniteRepeatable(tween(3400, easing = LinearEasing)), label = "a")
    val phaseB by transition.animateFloat(TAU, 0f, infiniteRepeatable(tween(5200, easing = LinearEasing)), label = "b")
    val phaseC by transition.animateFloat(0f, TAU, infiniteRepeatable(tween(7600, easing = LinearEasing)), label = "c")
    val slosh by transition.animateFloat(0f, TAU, infiniteRepeatable(tween(8800, easing = LinearEasing)), label = "slosh")
    val rise by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "rise")
    val drip by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(5600, easing = LinearEasing)), label = "drip")
    val light by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(11000, easing = LinearEasing)), label = "light")

    val surge = remember { Animatable(0f) }
    val ripple = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    Box(
        modifier.size(diameter)
            .semantics { contentDescription = "$description. Tap to splash." }
            .pointerInput(Unit) {
                detectTapGestures {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    scope.launch { surge.snapTo(1f); surge.animateTo(0f, tween(2200)) }
                    scope.launch { ripple.snapTo(0f); ripple.animateTo(1f, tween(1100)) }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2
            val circle = Path().apply { addOval(Rect(center, radius)) }
            drawCircle(Color.White.copy(alpha = 0.06f), radius)

            clipPath(circle) {
                val top = size.height * (1f - animatedLevel)
                val calm = (1f - (animatedLevel - 0.5f) * (animatedLevel - 0.5f) * 2.2f).coerceIn(0.35f, 1f)
                val sloshing = 1f + 0.4f * sin(slosh)
                val amp = 9.dp.toPx() * calm * sloshing * (1f + 1.8f * surge.value)
                val tilt = sin(slosh * 0.75f) * 12.dp.toPx() * calm + sin(slosh * 3f) * 5.dp.toPx() * surge.value

                // Back to front: deep, middle, surface.
                wave(top + 6.dp.toPx(), amp * 1.3f, 0.9f, phaseC, tilt * 0.6f, water.copy(alpha = 0.32f))
                wave(top + 2.dp.toPx(), amp * 1.1f, 1.3f, phaseB, tilt * 0.8f, water.copy(alpha = 0.55f))
                wave(top, amp, 1.8f, phaseA, tilt, water.copy(alpha = 0.92f), foam = true)

                // Depth: darker toward the bottom, brighter just under the surface.
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.16f), Color.Transparent, Color.Black.copy(alpha = 0.22f)),
                        startY = top, endY = size.height,
                    ),
                    topLeft = Offset(0f, top), size = Size(size.width, size.height - top),
                )

                // Dappled light drifting through the water.
                for (i in 0 until 5) {
                    val p = (light + i / 5f) % 1f
                    val x = size.width * (0.1f + 0.8f * hash01(i, 11)) + sin(p * TAU + i) * 30.dp.toPx()
                    val y = top + (size.height - top) * (0.25f + 0.6f * hash01(i, 12)) + sin(p * TAU * 2f) * 8.dp.toPx()
                    val r = (28f + hash01(i, 13) * 40f).dp.toPx()
                    drawCircle(Color.White.copy(alpha = 0.05f + 0.04f * sin(p * TAU).coerceAtLeast(0f)), r, Offset(x, y))
                }

                // Bubbles streaming up, wobbling as they go.
                for (i in 0 until 16) {
                    val speed = 0.55f + hash01(i, 21) * 0.9f
                    val progress = (rise * speed + hash01(i, 22)) % 1f
                    val x = size.width * (0.14f + 0.72f * hash01(i, 23)) + sin(progress * TAU * 3f + i) * 9.dp.toPx()
                    val y = size.height - progress * (size.height - top)
                    if (y > top + 6.dp.toPx()) {
                        val r = (2f + hash01(i, 24) * 5f).dp.toPx()
                        val a = 0.45f * (1f - progress)
                        drawCircle(Color.White.copy(alpha = a * 0.55f), r, Offset(x, y))
                        drawCircle(Color.White.copy(alpha = a), r, Offset(x, y), style = Stroke(1.2.dp.toPx()))
                    }
                }

                // A drip falls from the top of the glass and ripples the surface where it lands.
                val dropX = size.width * 0.5f + sin(drip * 3f) * size.width * 0.08f
                if (drip < 0.28f) {
                    val t = drip / 0.28f
                    val y = size.height * 0.04f + (top - size.height * 0.04f) * t * t
                    val h = 11.dp.toPx()
                    drawPath(
                        Path().apply {
                            moveTo(dropX, y - h)
                            cubicTo(dropX + h * 0.55f, y - h * 0.1f, dropX + h * 0.5f, y + h * 0.5f, dropX, y + h * 0.5f)
                            cubicTo(dropX - h * 0.5f, y + h * 0.5f, dropX - h * 0.55f, y - h * 0.1f, dropX, y - h)
                            close()
                        },
                        Color.White.copy(alpha = 0.8f),
                    )
                } else if (drip < 0.62f) {
                    val t = (drip - 0.28f) / 0.34f
                    drawOval(
                        Color.White.copy(alpha = 0.5f * (1f - t)),
                        topLeft = Offset(dropX - 90.dp.toPx() * t, top - 10.dp.toPx() * t),
                        size = Size(180.dp.toPx() * t, 20.dp.toPx() * t),
                        style = Stroke(2.dp.toPx()),
                    )
                }

                // Tap ripple spreading from the middle of the surface.
                if (ripple.value < 1f) {
                    val t = ripple.value
                    for (ring in 0 until 3) {
                        val rt = (t - ring * 0.14f).coerceIn(0f, 1f)
                        if (rt > 0f) {
                            drawOval(
                                Color.White.copy(alpha = 0.55f * (1f - rt)),
                                topLeft = Offset(size.width / 2 - 140.dp.toPx() * rt, top - 24.dp.toPx() * rt),
                                size = Size(280.dp.toPx() * rt, 48.dp.toPx() * rt),
                                style = Stroke(2.5.dp.toPx()),
                            )
                        }
                    }
                }
            }

            // Glass rim, inner edge shine and a soft highlight.
            drawCircle(Color.White.copy(alpha = 0.28f), radius - 1.5.dp.toPx(), style = Stroke(3.dp.toPx()))
            drawCircle(Color.Black.copy(alpha = 0.18f), radius - 8.dp.toPx(), style = Stroke(10.dp.toPx()))
            drawArc(
                Color.White.copy(alpha = 0.26f), 200f, 52f, false,
                Offset(size.width * 0.1f, size.height * 0.1f), Size(size.width * 0.8f, size.height * 0.8f),
                style = Stroke(5.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                Color.White.copy(alpha = 0.14f), 292f, 18f, false,
                Offset(size.width * 0.1f, size.height * 0.1f), Size(size.width * 0.8f, size.height * 0.8f),
                style = Stroke(4.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        content()
    }
}
