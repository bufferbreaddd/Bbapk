package dev.colorup

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import kotlin.math.roundToInt

internal fun Context.dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
internal fun Context.tone(attr: Int) = MaterialColors.getColor(this, attr, "ColorUp")
internal fun Context.column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
internal fun Context.textLabel(value: String, size: Int = 14, bold: Boolean = false, secondary: Boolean = false) = TextView(this).apply {
    text = value
    textSize = size.toFloat()
    setTextColor(tone(if (secondary) com.google.android.material.R.attr.colorOnSurfaceVariant else com.google.android.material.R.attr.colorOnSurface))
    if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    setPadding(0, dp(4), 0, dp(4))
}
internal fun Context.surface(body: LinearLayout.() -> Unit): MaterialCardView = MaterialCardView(this).apply {
    radius = dp(24).toFloat()
    cardElevation = 0f
    strokeWidth = 0
    setCardBackgroundColor(tone(com.google.android.material.R.attr.colorSurfaceContainer))
    addView(column().apply { setPadding(dp(20), dp(16), dp(20), dp(16)); body() })
    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
}

/** Uses the same start-to-rest convention and easing as the runtime. */
class MotionPreview(context: Context, private val spec: () -> AnimationSpec) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val card = RectF()
    private val outlineColor = context.tone(com.google.android.material.R.attr.colorOutlineVariant)
    private val containerColor = context.tone(com.google.android.material.R.attr.colorPrimaryContainer)
    private val contentColor = context.tone(com.google.android.material.R.attr.colorOnPrimaryContainer)
    private var progress = 1f
    private var animator: ValueAnimator? = null
    init { contentDescription = "Motion preview. Tap Play preview to animate."; minimumHeight = context.dp(180) }
    fun play() {
        animator?.cancel()
        val current = spec()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = current.durationMs.toLong()
            startDelay = current.delayMs.toLong()
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener { progress = it.animatedValue as Float; invalidate() }
            start()
        }
    }
    override fun onDetachedFromWindow() { animator?.cancel(); super.onDetachedFromWindow() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val current = spec()
        val amount = MotionMath.ease(progress, current.curve, current.springDamping, current.springStiffness)
        card.set(width / 2f - 64 * density, height / 2f - 38 * density, width / 2f + 64 * density, height / 2f + 38 * density)
        paint.color = outlineColor
        paint.style = Paint.Style.STROKE; paint.strokeWidth = density
        canvas.drawRoundRect(card, 18 * density, 18 * density, paint)
        paint.style = Paint.Style.FILL
        val scale = current.scale + (1f - current.scale) * amount
        val pivotX = when (current.direction) { "LEFT" -> card.right; "RIGHT" -> card.left; else -> card.centerX() }
        val pivotY = when (current.direction) { "UP" -> card.bottom; "DOWN" -> card.top; else -> card.centerY() }
        canvas.save()
        canvas.translate(0f, current.translateY * density * (1f - amount))
        canvas.scale(scale, scale, pivotX, pivotY)
        paint.color = containerColor
        paint.alpha = ((1f - current.fade * (1f - amount)).coerceIn(0f, 1f) * 255).roundToInt()
        canvas.drawRoundRect(card, 18 * density, 18 * density, paint)
        paint.color = contentColor
        paint.alpha = ((1f - current.fade * (1f - amount)).coerceIn(0f, 1f) * 255).roundToInt()
        canvas.drawCircle(card.left + 23 * density, card.centerY(), 9 * density, paint)
        canvas.drawRoundRect(card.left + 42 * density, card.top + 25 * density, card.right - 16 * density, card.top + 31 * density, 3 * density, 3 * density, paint)
        paint.alpha = (paint.alpha * 0.45f).roundToInt()
        canvas.drawRoundRect(card.left + 42 * density, card.top + 40 * density, card.right - 32 * density, card.top + 45 * density, 3 * density, 3 * density, paint)
        paint.alpha = 255
        canvas.restore()
    }
}

class CurvePreview(context: Context, private val spec: () -> AnimationSpec) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    init { contentDescription = "Selected easing curve"; minimumHeight = context.dp(100) }
    override fun onDraw(canvas: Canvas) {
        val inset = context.dp(8).toFloat()
        val w = width - inset * 2; val h = height - inset * 2
        paint.color = context.tone(com.google.android.material.R.attr.colorOutlineVariant)
        paint.strokeWidth = context.dp(1).toFloat()
        canvas.drawLine(inset, inset + h, inset + w, inset + h, paint)
        val s = spec()
        paint.color = context.tone(com.google.android.material.R.attr.colorPrimary)
        paint.strokeWidth = context.dp(3).toFloat()
        var lastX = inset; var lastY = inset + h
        for (i in 1..100) {
            val t = i / 100f
            val x = inset + w * t
            val y = inset + h - h * MotionMath.ease(t, s.curve, s.springDamping, s.springStiffness) / 1.15f
            canvas.drawLine(lastX, lastY, x, y, paint)
            lastX = x; lastY = y
        }
    }
}
