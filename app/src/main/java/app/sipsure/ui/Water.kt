package app.sipsure.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.drawscope.rotate
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
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

private const val TAU = (2 * PI).toFloat()

/** Cheap deterministic 0..1 value per index, so bubbles look random but don't jump between frames. */
private fun hash01(i: Int, salt: Int): Float {
    var h = i * 374761393 + salt * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    return ((h xor (h ushr 16)) and 0xFFFF) / 65535f
}

/** Draws a wave band whose top edge follows a sine curve, optionally with a foam line on the surface. */
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
    if (foam) drawPath(line, Color.White.copy(alpha = 0.45f), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
}

// ---------- Full-screen backdrop ----------

/**
 * A deep, quiet page: near-black navy with one soft pool of light behind the hero that takes the
 * current state's colour, and a few motes drifting up like bubbles far away.
 */
@Composable
fun OceanBackground(modifier: Modifier = Modifier, glow: Color = Ocean.Aqua, content: @Composable BoxScope.() -> Unit) {
    val tint by animateColorAsState(glow, tween(900), label = "glow")
    val transition = rememberInfiniteTransition(label = "ocean")
    val drift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(30000, easing = LinearEasing)), label = "drift")
    val breathe by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(6000), RepeatMode.Reverse), label = "breathe")

    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ocean.BgTop, Ocean.BgMid, Ocean.BgBottom)))) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height * 0.28f)
            val r = size.width * (0.95f + 0.05f * breathe)
            drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = 0.16f + 0.04f * breathe), Color.Transparent), c, r), r, c)
            val low = Offset(size.width * 0.1f, size.height * 1.02f)
            drawCircle(Brush.radialGradient(listOf(Ocean.AquaDeep.copy(alpha = 0.10f), Color.Transparent), low, size.width * 0.9f), size.width * 0.9f, low)

            for (i in 0 until 14) {
                val speed = 0.5f + hash01(i, 1) * 0.8f
                val progress = (drift * speed + hash01(i, 2)) % 1f
                val x = size.width * hash01(i, 3) + sin(progress * TAU * 2f + i) * 10.dp.toPx()
                val y = size.height * (1.05f - progress * 1.15f)
                val fade = (1f - abs(progress - 0.5f) * 2f).coerceIn(0f, 1f)
                drawCircle(Color.White.copy(alpha = 0.10f * fade), (1f + hash01(i, 4) * 2.5f).dp.toPx(), Offset(x, y))
            }
        }
        content()
    }
}

// ---------- The water orb ----------

/**
 * The centrepiece: an orb of living water inside a progress ring. Three wave layers roll at
 * different speeds, the surface sloshes and tilts, drips fall in and ripple the surface, light
 * dapples the depths and bubbles rise. Tapping splashes it.
 *
 * [level] is 0..1 and animates with a spring. [progress] (0..1, or null for no ring) is how far
 * through the current period we are. [content] is drawn centred on top.
 */
@Composable
fun WaterGlass(
    level: Float,
    water: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 290.dp,
    progress: Float? = null,
    description: String,
    content: @Composable BoxScope.() -> Unit,
) {
    val animatedLevel by animateFloatAsState(level.coerceIn(0f, 1f), spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessVeryLow), label = "level")
    val animatedProgress by animateFloatAsState((progress ?: 0f).coerceIn(0f, 1f), tween(900), label = "progress")
    val tint by animateColorAsState(water, tween(700), label = "water")
    val transition = rememberInfiniteTransition(label = "water")
    val phaseA by transition.animateFloat(0f, TAU, infiniteRepeatable(tween(3400, easing = LinearEasing)), label = "a")
    val phaseB by transition.animateFloat(TAU, 0f, infiniteRepeatable(tween(5200, easing = LinearEasing)), label = "b")
    val phaseC by transition.animateFloat(0f, TAU, infiniteRepeatable(tween(7600, easing = LinearEasing)), label = "c")
    val slosh by transition.animateFloat(0f, TAU, infiniteRepeatable(tween(8800, easing = LinearEasing)), label = "slosh")
    val rise by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "rise")
    val drip by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(6400, easing = LinearEasing)), label = "drip")
    val light by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(11000, easing = LinearEasing)), label = "light")
    val pulse by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "pulse")

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
            val outer = size.minDimension / 2
            val ringWidth = 6.dp.toPx()
            val ringRadius = outer - ringWidth
            val radius = outer - 22.dp.toPx()

            // Soft glow of the water colour behind the orb.
            drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = 0.30f), Color.Transparent), center, outer), outer)

            // Progress ring: hairline track, gradient arc, and a glowing head.
            drawCircle(Color.White.copy(alpha = 0.07f), ringRadius, style = Stroke(ringWidth))
            if (progress != null && animatedProgress > 0.002f) {
                val sweep = 360f * animatedProgress
                rotate(-90f) {
                    drawArc(
                        Brush.sweepGradient(listOf(tint.copy(alpha = 0.35f), tint, Color.White.copy(alpha = 0.95f)), center),
                        startAngle = 0f, sweepAngle = sweep, useCenter = false,
                        topLeft = Offset(center.x - ringRadius, center.y - ringRadius), size = Size(ringRadius * 2, ringRadius * 2),
                        style = Stroke(ringWidth, cap = StrokeCap.Round),
                    )
                }
                val a = (sweep - 90f) * PI.toFloat() / 180f
                val head = Offset(center.x + cos(a) * ringRadius, center.y + sin(a) * ringRadius)
                drawCircle(tint.copy(alpha = 0.35f + 0.25f * pulse), ringWidth * (1.6f + 0.5f * pulse), head)
                drawCircle(Color.White, ringWidth * 0.62f, head)
            }

            // The glass body.
            val circle = Path().apply { addOval(Rect(center, radius)) }
            drawCircle(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.02f)), center.y - radius, center.y + radius),
                radius,
            )

            clipPath(circle) {
                val box = Rect(center, radius)
                val top = box.bottom - box.height * animatedLevel
                val calm = (1f - (animatedLevel - 0.5f) * (animatedLevel - 0.5f) * 2.2f).coerceIn(0.35f, 1f)
                val sloshing = 1f + 0.4f * sin(slosh)
                val amp = 8.dp.toPx() * calm * sloshing * (1f + 1.8f * surge.value)
                val tilt = sin(slosh * 0.75f) * 12.dp.toPx() * calm + sin(slosh * 3f) * 5.dp.toPx() * surge.value

                // Back to front: deep, middle, surface.
                wave(top + 7.dp.toPx(), amp * 1.3f, 0.9f, phaseC, tilt * 0.6f, tint.copy(alpha = 0.30f))
                wave(top + 3.dp.toPx(), amp * 1.1f, 1.3f, phaseB, tilt * 0.8f, tint.copy(alpha = 0.55f))
                wave(top, amp, 1.8f, phaseA, tilt, tint.copy(alpha = 0.92f), foam = true)

                // Depth: brighter just under the surface, darker toward the bottom.
                drawRect(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent, Color.Black.copy(alpha = 0.28f)), startY = top, endY = box.bottom),
                    topLeft = Offset(0f, top), size = Size(size.width, size.height - top),
                )

                // Dappled light drifting through the water.
                for (i in 0 until 5) {
                    val p = (light + i / 5f) % 1f
                    val x = box.left + box.width * (0.1f + 0.8f * hash01(i, 11)) + sin(p * TAU + i) * 26.dp.toPx()
                    val y = top + (box.bottom - top) * (0.25f + 0.6f * hash01(i, 12)) + sin(p * TAU * 2f) * 8.dp.toPx()
                    val r = (24f + hash01(i, 13) * 34f).dp.toPx()
                    drawCircle(Color.White.copy(alpha = 0.04f + 0.04f * sin(p * TAU).coerceAtLeast(0f)), r, Offset(x, y))
                }

                // Bubbles streaming up, wobbling as they go.
                for (i in 0 until 14) {
                    val speed = 0.55f + hash01(i, 21) * 0.9f
                    val prog = (rise * speed + hash01(i, 22)) % 1f
                    val x = box.left + box.width * (0.16f + 0.68f * hash01(i, 23)) + sin(prog * TAU * 3f + i) * 8.dp.toPx()
                    val y = box.bottom - prog * (box.bottom - top)
                    if (y > top + 6.dp.toPx()) {
                        val r = (1.5f + hash01(i, 24) * 4f).dp.toPx()
                        val a = 0.5f * (1f - prog)
                        drawCircle(Color.White.copy(alpha = a * 0.5f), r, Offset(x, y))
                        drawCircle(Color.White.copy(alpha = a), r, Offset(x, y), style = Stroke(1.dp.toPx()))
                    }
                }

                // A drip falls from the top of the orb and ripples the surface where it lands.
                val dropX = center.x + sin(drip * 3f) * box.width * 0.08f
                if (drip < 0.26f) {
                    val t = drip / 0.26f
                    val start = box.top + box.height * 0.06f
                    val y = start + (top - start) * t * t
                    val h = 9.dp.toPx()
                    drawPath(
                        Path().apply {
                            moveTo(dropX, y - h)
                            cubicTo(dropX + h * 0.55f, y - h * 0.1f, dropX + h * 0.5f, y + h * 0.5f, dropX, y + h * 0.5f)
                            cubicTo(dropX - h * 0.5f, y + h * 0.5f, dropX - h * 0.55f, y - h * 0.1f, dropX, y - h)
                            close()
                        },
                        Color.White.copy(alpha = 0.6f),
                    )
                } else if (drip < 0.6f) {
                    val t = (drip - 0.26f) / 0.34f
                    drawOval(
                        Color.White.copy(alpha = 0.45f * (1f - t)),
                        topLeft = Offset(dropX - 80.dp.toPx() * t, top - 9.dp.toPx() * t),
                        size = Size(160.dp.toPx() * t, 18.dp.toPx() * t),
                        style = Stroke(1.5.dp.toPx()),
                    )
                }

                // Tap ripple spreading from the middle of the surface.
                if (ripple.value < 1f) {
                    val t = ripple.value
                    for (ring in 0 until 3) {
                        val rt = (t - ring * 0.14f).coerceIn(0f, 1f)
                        if (rt > 0f) {
                            drawOval(
                                Color.White.copy(alpha = 0.5f * (1f - rt)),
                                topLeft = Offset(center.x - 120.dp.toPx() * rt, top - 20.dp.toPx() * rt),
                                size = Size(240.dp.toPx() * rt, 40.dp.toPx() * rt),
                                style = Stroke(2.dp.toPx()),
                            )
                        }
                    }
                }
            }

            // Thin rim and a soft specular highlight, like real glass.
            drawCircle(Color.White.copy(alpha = 0.16f), radius, style = Stroke(1.dp.toPx()))
            drawArc(
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.30f), Color.Transparent), Offset(center.x - radius, center.y - radius), center),
                195f, 70f, false,
                Offset(center.x - radius * 0.86f, center.y - radius * 0.86f), Size(radius * 1.72f, radius * 1.72f),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        content()
    }
}
