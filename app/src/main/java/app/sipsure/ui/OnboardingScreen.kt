package app.sipsure.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sipsure.domain.Phase
import app.sipsure.domain.Schedule
import app.sipsure.ui.theme.Ocean

private const val STEPS = 4

/** Four short, mostly visual steps: promise, how it works, privacy, schedule. */
@Composable
fun OnboardingScreen(host: AppHost, modifier: Modifier = Modifier) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var consented by rememberSaveable { mutableStateOf(false) }
    var schedule by remember { mutableStateOf(Schedule()) }

    Column(modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(STEPS) { index ->
                Box(
                    Modifier.padding(end = 6.dp).height(5.dp).width(if (index == step) 30.dp else 10.dp).clip(CircleShape)
                        .background(if (index <= step) Ocean.Aqua else Color.White.copy(alpha = 0.18f)),
                )
            }
            Spacer(Modifier.weight(1f))
            if (step > 0) TextButton({ step -= 1 }) { Text("Back", color = Ocean.TextMid) }
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = { (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut()) },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            label = "onboarding",
        ) { current ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                when (current) {
                    0 -> Promise()
                    1 -> HowItWorks()
                    2 -> Privacy(consented) { consented = it }
                    else -> ScheduleStep(schedule) { schedule = it }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        when (step) {
            0 -> PillButton("Get started", { step = 1 })
            1 -> PillButton("Continue", { step = 2 })
            2 -> PillButton("Continue", { if (consented) step = 3 }, kind = if (consented) ButtonKind.Primary else ButtonKind.Glass)
            else -> PillButton("Start my schedule", {
                host.act { completeSetup(schedule) }
                if (host.controller.state.policy.phase != Phase.SETUP_REQUIRED) host.requestNotifications()
            }) { CheckGlyph(it) }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Promise() {
    Spacer(Modifier.height(8.dp))
    WaterGlass(level = 0.62f, water = Ocean.WaterCalm, diameter = 280.dp, description = "Illustration of a glass of water") {
        DropGlyph(Color.White, 72.dp)
    }
    Spacer(Modifier.height(28.dp))
    Text("Drink water,\non purpose.", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp, lineHeight = 44.sp), color = Ocean.TextHigh, textAlign = TextAlign.Center)
    Spacer(Modifier.height(12.dp))
    Text(
        "Gentle reminders, a quick photo, and a little accountability to build the habit.",
        style = MaterialTheme.typography.bodyLarge, color = Ocean.TextMid, textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))
    Text(
        "A habit tool, not medical advice. Don't drink more than is comfortable, and follow your doctor's guidance.",
        style = MaterialTheme.typography.bodyMedium, color = Ocean.TextLow, textAlign = TextAlign.Center,
    )
}

@Composable
private fun HowItWorks() {
    Spacer(Modifier.height(16.dp))
    Text("How it works", style = MaterialTheme.typography.headlineLarge, color = Ocean.TextHigh, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(20.dp))
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StepCard("1", "You get a nudge", "At the interval you choose. Quiet hours keep nights free.") { DropGlyph(it, 24.dp) }
        StepCard("2", "Snap a sip", "Take a quick photo of you drinking. It's checked on your phone, then deleted.") { CameraGlyph(it) }
        StepCard("3", "Missed one?", "A restriction screen appears. A credit lifts it, and calls always work. If the optional phone lock is on, only a credit unlocks the phone.") { LockGlyph(it) }
    }
}

@Composable
private fun StepCard(number: String, title: String, body: String, glyph: @Composable (Color) -> Unit) {
    GlassCard {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Ocean.Aqua.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { glyph(Ocean.Aqua) }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("$number · $title", style = MaterialTheme.typography.titleLarge, color = Ocean.TextHigh)
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = Ocean.TextMid)
            }
        }
    }
}

@Composable
private fun Privacy(consented: Boolean, onConsent: (Boolean) -> Unit) {
    Spacer(Modifier.height(16.dp))
    Text("Privacy, plainly", style = MaterialTheme.typography.headlineLarge, color = Ocean.TextHigh, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(20.dp))
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Fact("Photos are checked on your phone and deleted. Nothing is uploaded.")
            Fact("Only a tiny fingerprint is kept, to spot repeat photos.")
            Fact("Photo checks can't prove you drank water, or how much.")
            Fact("Credits are free test credits. No real money is charged.")
            Fact("The optional phone lock is off unless you set it up. When on, only a credit unlocks the phone, and emergency calls always work.")
            Fact("A borrowed credit must be repaid within 24 hours. While one is unpaid, SipSure can't be uninstalled on a phone set up for the lock.")
        }
    }
    Spacer(Modifier.height(16.dp))
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(if (consented) Ocean.Aqua.copy(alpha = 0.16f) else Ocean.Glass)
            .border(1.dp, if (consented) Ocean.Aqua else Ocean.GlassBorder, shape)
            .springClick(role = Role.Checkbox) { onConsent(!consented) }.padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(if (consented) Ocean.Aqua else Color.Transparent)
                .border(2.dp, if (consented) Ocean.Aqua else Ocean.TextLow, CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (consented) CheckGlyph(Color(0xFF00232F), 18.dp) }
        Spacer(Modifier.width(14.dp))
        Text("I understand and I'm happy with this.", style = MaterialTheme.typography.titleMedium, color = Ocean.TextHigh)
    }
}

@Composable
private fun Fact(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 2.dp)) { CheckGlyph(Ocean.Aqua, 20.dp) }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Ocean.TextHigh)
    }
}

@Composable
private fun ScheduleStep(schedule: Schedule, onChange: (Schedule) -> Unit) {
    Spacer(Modifier.height(16.dp))
    Text("Set your rhythm", style = MaterialTheme.typography.headlineLarge, color = Ocean.TextHigh, modifier = Modifier.fillMaxWidth())
    Text("Change any of this later in Settings.", style = MaterialTheme.typography.bodyLarge, color = Ocean.TextMid, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(20.dp))
    GlassCard(padding = 20.dp) { ScheduleEditor(schedule, onChange) }
    Spacer(Modifier.height(14.dp))
    Text(
        "Every ${intervalLabel(schedule.intervalMinutes)} you'll have ${schedule.checkInWindowMinutes} min to check in" +
            if (schedule.graceMinutes > 0) ", plus ${schedule.graceMinutes} min of grace." else ".",
        style = MaterialTheme.typography.bodyLarge, color = Ocean.Foam, textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
}
