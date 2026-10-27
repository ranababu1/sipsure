package app.sipsure.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.sipsure.domain.Schedule
import kotlin.math.roundToInt

/** Slider-based schedule form shared by onboarding and settings. Values always stay within valid steps. */
@Composable
fun ScheduleEditor(value: Schedule, onChange: (Schedule) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SliderRow(
            "Remind me every", intervalLabel(value.intervalMinutes),
            value.intervalMinutes, Schedule.MIN_INTERVAL..Schedule.MAX_INTERVAL, 15,
        ) { onChange(value.copy(intervalMinutes = it)) }
        SliderRow(
            "Check-in window", "${value.checkInWindowMinutes} min",
            value.checkInWindowMinutes, 5..60, 5,
        ) { onChange(value.copy(checkInWindowMinutes = it)) }
        SliderRow(
            "Grace period", if (value.graceMinutes == 0) "None" else "${value.graceMinutes} min",
            value.graceMinutes, 0..60, 5,
        ) { onChange(value.copy(graceMinutes = it)) }

        val quietOn = value.quietStartMinuteOfDay != value.quietEndMinuteOfDay
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Quiet hours", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (quietOn) "No reminders ${hourLabel(value.quietStartMinuteOfDay / 60)} – ${hourLabel(value.quietEndMinuteOfDay / 60)}"
                    else "Reminders can arrive at any hour",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = quietOn,
                onCheckedChange = { on ->
                    onChange(
                        if (on) value.copy(quietStartMinuteOfDay = 22 * 60, quietEndMinuteOfDay = 7 * 60)
                        else value.copy(quietEndMinuteOfDay = value.quietStartMinuteOfDay),
                    )
                },
                modifier = Modifier.semantics { contentDescription = "Quiet hours" },
            )
        }
        if (quietOn) {
            SliderRow("From", hourLabel(value.quietStartMinuteOfDay / 60), value.quietStartMinuteOfDay / 60, 0..23, 1) {
                onChange(value.copy(quietStartMinuteOfDay = it * 60))
            }
            SliderRow("Until", hourLabel(value.quietEndMinuteOfDay / 60), value.quietEndMinuteOfDay / 60, 0..23, 1) {
                onChange(value.copy(quietEndMinuteOfDay = it * 60))
            }
        }
        value.validationError()?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SliderRow(label: String, valueText: String, value: Int, range: IntRange, step: Int, onChange: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(valueText, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        val steps = ((range.last - range.first) / step - 1).coerceAtLeast(0)
        Slider(
            value = value.toFloat(),
            onValueChange = { raw ->
                val snapped = range.first + ((raw - range.first) / step).roundToInt() * step
                onChange(snapped.coerceIn(range))
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = steps,
            modifier = Modifier.semantics { contentDescription = "$label, $valueText" },
        )
    }
}
