package app.sipsure

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/** Placeholder shell; onboarding and schedule UI come in later tasks. */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = getString(R.string.tagline)
            textSize = 20f
            setPadding(48, 96, 48, 48)
        })
    }
}
