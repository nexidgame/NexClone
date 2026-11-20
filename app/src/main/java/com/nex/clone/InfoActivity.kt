package com.nex.clone

import android.os.Bundle
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.nex.clone.databinding.ActivityInfoBinding
import com.nex.clone.ui.applySystemBarsPadding
import com.nex.clone.ui.dp

/** "Pertanyaan Umum" dan "Tentang Kami". */
class InfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarsPadding()
        b.toolbar.setNavigationOnClickListener { finish() }

        if (intent.getStringExtra(EXTRA_MODE) == MODE_ABOUT) {
            b.toolbar.setTitle(R.string.uc_about)
            buildAbout(b.container)
        } else {
            b.toolbar.setTitle(R.string.uc_faq)
            buildFaq(b.container)
        }
    }

    private fun text(c: LinearLayout, s: String, size: Float, bold: Boolean, color: Int, topDp: Int) {
        c.addView(TextView(this).apply {
            text = s
            textSize = size
            setTextColor(ContextCompat.getColor(this@InfoActivity, color))
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setLineSpacing(0f, 1.15f)
            setPadding(0, dp(topDp), 0, 0)
        })
    }

    private fun buildFaq(c: LinearLayout) {
        val qs = intArrayOf(R.string.faq_q1, R.string.faq_q2, R.string.faq_q3, R.string.faq_q4, R.string.faq_q5, R.string.faq_q6)
        val ans = intArrayOf(R.string.faq_a1, R.string.faq_a2, R.string.faq_a3, R.string.faq_a4, R.string.faq_a5, R.string.faq_a6)
        for (i in qs.indices) {
            text(c, getString(qs[i]), 16f, true, R.color.on_surface, if (i == 0) 0 else 20)
            text(c, getString(ans[i]), 14f, false, R.color.on_surface_variant, 6)
        }
    }

    private fun buildAbout(c: LinearLayout) {
        val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: ""
        text(c, getString(R.string.app_name), 26f, true, R.color.on_surface, 0)
        text(c, getString(R.string.version_fmt, version), 13f, false, R.color.on_surface_variant, 2)
        text(c, getString(R.string.about_desc), 15f, false, R.color.on_surface, 18)
        text(c, getString(R.string.about_limits_title), 16f, true, R.color.secondary, 22)
        text(c, getString(R.string.about_limits), 14f, false, R.color.on_surface_variant, 6)
        text(c, packageName, 12f, false, R.color.on_surface_variant, 22)
    }

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_FAQ = "faq"
        const val MODE_ABOUT = "about"
    }
}
