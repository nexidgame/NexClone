package com.nex.clone.core

import android.content.Context
import android.content.Intent
import android.os.Build
import com.nex.clone.R

/** Laporan bug, umpan balik, dan berbagi — lewat pemilih aplikasi (tanpa alamat email tetap). */
object Support {

    private fun versionName(ctx: Context): String =
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "?"

    fun deviceInfo(ctx: Context, clonePkg: String?): String = buildString {
        appendLine("Nex Clone ${versionName(ctx)}")
        appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Profil klon: ${if (Profiles.cloneUser(ctx) != null) "siap" else "belum siap"}")
        if (clonePkg != null) appendLine("Paket klon: $clonePkg")
    }

    private fun send(ctx: Context, subject: String, body: String, chooserTitle: String) {
        val i = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, body)
        ctx.startActivity(Intent.createChooser(i, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun sendBugReport(ctx: Context, clonePkg: String?) = send(
        ctx, ctx.getString(R.string.bug_subject),
        ctx.getString(R.string.bug_body_intro) + "\n\n" + deviceInfo(ctx, clonePkg),
        ctx.getString(R.string.menu_bug),
    )

    fun sendFeedback(ctx: Context) = send(
        ctx, ctx.getString(R.string.feedback_subject),
        "\n\n" + deviceInfo(ctx, null), ctx.getString(R.string.uc_feedback),
    )

    fun shareApp(ctx: Context) = send(
        ctx, ctx.getString(R.string.app_name), ctx.getString(R.string.share_text), ctx.getString(R.string.uc_share),
    )
}
