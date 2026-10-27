package app.sipsure.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A teardrop, the app's one illustration motif. */
@Composable
fun WaterDrop(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w / 2, 0f)
            cubicTo(w * 0.98f, h * 0.48f, w * 0.88f, h, w / 2, h)
            cubicTo(w * 0.12f, h, w * 0.02f, h * 0.48f, w / 2, 0f)
            close()
        }
        drawPath(path, color)
        drawPath(
            Path().apply {
                moveTo(w * 0.32f, h * 0.62f)
                cubicTo(w * 0.3f, h * 0.76f, w * 0.38f, h * 0.84f, w * 0.48f, h * 0.86f)
            },
            Color.Black.copy(alpha = 0.18f),
            style = Stroke(width = w * 0.06f, cap = StrokeCap.Round),
        )
    }
}

/** Circular progress with rounded caps; [content] is centred inside. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 236.dp,
    strokeWidth: Dp = 16.dp,
    trackColor: Color = Color.White.copy(alpha = 0.22f),
    barColor: Color = Color.White,
    description: String,
    content: @Composable BoxScope.() -> Unit,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "ring")
    Box(modifier.size(diameter).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(trackColor, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            if (animated > 0f) {
                drawArc(barColor, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        content()
    }
}

/** Large rounded gradient card used for the one thing that matters on a screen. */
@Composable
fun HeroCard(colors: List<Color>, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(colors)),
    ) {
        // Soft decorative bubbles keep the flat gradient from feeling empty.
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.07f), radius = size.width * 0.42f, center = Offset(size.width * 0.95f, size.height * 0.05f))
            drawCircle(Color.White.copy(alpha = 0.05f), radius = size.width * 0.3f, center = Offset(size.width * 0.02f, size.height * 0.98f))
        }
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
fun StatusPill(text: String, modifier: Modifier = Modifier, container: Color = Color.White.copy(alpha = 0.2f), content: Color = Color.White) {
    Surface(modifier, shape = CircleShape, color = container, contentColor = content) {
        Text(text, Modifier.padding(horizontal = 14.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun SoftCard(modifier: Modifier = Modifier, container: Color = MaterialTheme.colorScheme.surface, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(top = 8.dp, bottom = 2.dp), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, container: Color = MaterialTheme.colorScheme.primary, content: Color = MaterialTheme.colorScheme.onPrimary) {
    Button(
        onClick, modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, tint: Color = MaterialTheme.colorScheme.primary) {
    OutlinedButton(
        onClick, modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = tint),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, container: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(44.dp).clip(CircleShape).background(container), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

/** An icon + title + supporting text row. */
@Composable
fun InfoRow(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
        IconBadge(icon, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
