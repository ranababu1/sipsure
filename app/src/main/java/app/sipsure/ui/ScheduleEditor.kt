package app.sipsure.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.sipsure.domain.Schedule
import app.sipsure.ui.theme.Ocean

private data class Quiet(val label: String, val start: Int, val end: Int)

private val quietPresets = listOf(
    Quiet("Off", 0, 0),
    Quiet("22:00 – 07:00", 22 * 60, 7 * 60),
    Quiet("23:00 – 08:00", 23 * 60, 8 * 60),
    Quiet("00:00 – 08:00", 0, 8 * 60),
)

/** Preset chips instead of free-form numbers: every combination is valid or visibly disabled. */
@Composable
fun ScheduleEditor(value: Schedule, onChange: (Schedule) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ChipGroup("Remind me every", listOf(30, 60, 90, 120, 180, 240, 360).map { it to intervalLabel(it) }, value.intervalMinutes) {
            onChange(value.copy(intervalMinutes = it))
        }
        ChipGroup(
            "Check-in window", listOf(5, 10, 15, 20, 30).map { it to "$it min" }, value.checkInWindowMinutes,
            enabled = { it < value.intervalMinutes },
        ) { onChange(value.copy(checkInWindowMinutes = it)) }
        ChipGroup("Grace period", listOf(0, 5, 10, 15, 30).map { it to if (it == 0) "None" else "$it min" }, value.graceMinutes) {
            onChange(value.copy(graceMinutes = it))
        }
        val current = quietPresets.firstOrNull { it.start == value.quietStartMinuteOfDay && it.end == value.quietEndMinuteOfDay }
            ?: if (value.quietStartMinuteOfDay == value.quietEndMinuteOfDay) quietPresets.first() else null
        ChipGroup("Quiet hours", quietPresets.mapIndexed { index, q -> index to q.label }, quietPresets.indexOf(current).takeIf { it >= 0 } ?: -1) {
            val preset = quietPresets[it]
            onChange(value.copy(quietStartMinuteOfDay = preset.start, quietEndMinuteOfDay = preset.end))
        }
        value.validationError()?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ChipGroup(
    title: String,
    options: List<Pair<Int, String>>,
    selected: Int,
    enabled: (Int) -> Boolean = { true },
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ocean.TextHigh)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (key, label) ->
                SelectChip(label, selected == key, { onSelect(key) }, enabled = enabled(key))
            }
        }
    }
}
