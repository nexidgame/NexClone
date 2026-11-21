package com.nex.clone.ui

import android.content.Context
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nex.clone.R
import com.nex.clone.core.OpResult
import com.nex.clone.core.ProfileOps
import com.nex.clone.core.message

/** Menampilkan kegagalan perintah ke profil kerja beserta alasan teknisnya (untuk diagnosis). */
object OpFeedback {

    fun failed(ctx: Context, r: OpResult, reason: String?, failedRes: Int) {
        if (reason != null && reason.startsWith(ProfileOps.REASON_OPENED_STORE)) {
            Toast.makeText(ctx, R.string.install_opened_store, Toast.LENGTH_LONG).show()
            return
        }
        val detail = reason?.takeIf { it.isNotBlank() }
        val text = r.message(ctx, failedRes) +
            (detail?.let { "\n\n" + ctx.getString(R.string.detail_fmt, it) } ?: "")
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.install_failed_title)
            .setMessage(text)
            .setPositiveButton(R.string.ok, null)
            .show()
    }
}
