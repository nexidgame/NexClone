package com.nex.clone.ui

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.nex.clone.R

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

/** Tambahkan padding sebesar system bar (edge-to-edge, wajib untuk targetSdk 35+). */
fun View.applySystemBarsPadding() {
    val l = paddingLeft
    val t = paddingTop
    val r = paddingRight
    val b = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        v.updatePadding(left = l + bars.left, top = t + bars.top, right = r + bars.right, bottom = b + bars.bottom)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

/** Dialog "sedang memproses" yang tidak bisa dibatalkan. */
object Busy {
    fun show(ctx: Context, msgRes: Int): AlertDialog {
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.dp(24), ctx.dp(22), ctx.dp(24), ctx.dp(22))
        }
        box.addView(CircularProgressIndicator(ctx).apply { isIndeterminate = true })
        box.addView(TextView(ctx).apply {
            setText(msgRes)
            textSize = 15f
            setTextColor(ctx.getColor(R.color.on_surface))
            setPadding(ctx.dp(20), 0, 0, 0)
        })
        return MaterialAlertDialogBuilder(ctx).setView(box).setCancelable(false).show()
    }
}
