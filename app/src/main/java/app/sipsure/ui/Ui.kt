package app.sipsure.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** Tiny view-building helpers so screens stay readable without adding a UI framework. */

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

fun column(context: Context, padding: Int = 0): LinearLayout = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(context.dp(padding), context.dp(padding), context.dp(padding), context.dp(padding))
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
}

private fun LinearLayout.add(view: View, topMarginDp: Int = 8): View {
    addView(
        view,
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = context.dp(topMarginDp) },
    )
    return view
}

fun LinearLayout.heading(text: String, sizeSp: Float = 24f): TextView =
    TextView(context).apply {
        this.text = text
        textSize = sizeSp
        setTypeface(typeface, Typeface.BOLD)
        setAccessibilityHeading(true)
    }.also { add(it, 16) } as TextView

fun LinearLayout.body(text: String, sizeSp: Float = 16f, topMarginDp: Int = 8): TextView =
    TextView(context).apply {
        this.text = text
        textSize = sizeSp
    }.also { add(it, topMarginDp) } as TextView

fun LinearLayout.caption(text: String): TextView = body(text, 13f, 4).apply { alpha = 0.75f }

/** A rounded, tinted panel. Text inside uses fixed dark ink so contrast holds in light and dark themes. */
fun LinearLayout.card(background: Int, build: LinearLayout.() -> Unit): LinearLayout =
    column(context, 16).apply {
        this.background = GradientDrawable().apply {
            setColor(background)
            cornerRadius = context.dp(12).toFloat()
        }
        build()
        for (i in 0 until childCount) (getChildAt(i) as? TextView)?.setTextColor(INK)
    }.also { add(it, 12) }

fun LinearLayout.button(text: String, primary: Boolean = false, onClick: () -> Unit): Button =
    Button(context).apply {
        this.text = text
        isAllCaps = false
        minimumHeight = context.dp(48)
        if (primary) {
            setBackgroundColor(ACCENT)
            setTextColor(Color.WHITE)
        }
        setOnClickListener { onClick() }
    }.also { add(it, 8) } as Button

fun LinearLayout.checkbox(text: String, onChange: (Boolean) -> Unit): CheckBox =
    CheckBox(context).apply {
        this.text = text
        textSize = 16f
        setOnCheckedChangeListener { _, checked -> onChange(checked) }
    }.also { add(it, 8) } as CheckBox

fun LinearLayout.numberField(label: String, value: Int): EditText {
    caption(label)
    return EditText(context).apply {
        setText(value.toString())
        inputType = InputType.TYPE_CLASS_NUMBER
        contentDescription = label
    }.also { add(it, 0) } as EditText
}

val ACCENT: Int = Color.parseColor("#0B6E99")
val INK: Int = Color.parseColor("#1B1B1B")
val CALM_BLUE: Int = Color.parseColor("#D6ECF7")
val AMBER: Int = Color.parseColor("#FFE9B8")
val SOFT_RED: Int = Color.parseColor("#F9D6D5")
val SOFT_GREEN: Int = Color.parseColor("#D4EED8")
val NEUTRAL: Int = Color.parseColor("#E6E6E6")
