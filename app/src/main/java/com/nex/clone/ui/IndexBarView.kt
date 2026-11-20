package com.nex.clone.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.nex.clone.R

/** Indeks huruf A–Z + # di sisi kanan daftar aplikasi. */
class IndexBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val letters: List<Char> = ('A'..'Z').toList() + '#'
    private val d = resources.displayMetrics.density
    private val itemH = 16f * d
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 11f * d
    }

    var onLetter: ((Char) -> Unit)? = null

    var active: Char? = null
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize((22f * d).toInt(), widthMeasureSpec),
            (itemH * letters.size).toInt(),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val offset = (paint.descent() + paint.ascent()) / 2f
        letters.forEachIndexed { i, c ->
            val isActive = c == active
            paint.color = ContextCompat.getColor(
                context, if (isActive) R.color.primary else R.color.on_surface_variant
            )
            paint.typeface = if (isActive) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            canvas.drawText(c.toString(), cx, itemH * i + itemH / 2f - offset, paint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val i = (e.y / itemH).toInt().coerceIn(0, letters.size - 1)
                onLetter?.invoke(letters[i])
            }
        }
        return true
    }
}
