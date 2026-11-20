package com.nex.clone.core

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.nex.clone.LaunchCloneActivity

object Shortcuts {

    /** Pintasan desktop untuk meluncurkan klon (melewati kunci aplikasi bila aktif). */
    fun pin(ctx: Context, pkg: String, label: String, icon: Bitmap): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(ctx)) return false
        val intent = Intent(ctx, LaunchCloneActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(LaunchCloneActivity.EXTRA_PKG, pkg)
        val info = ShortcutInfoCompat.Builder(ctx, "clone_$pkg")
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(IconCompat.createWithBitmap(icon))
            .setIntent(intent)
            .build()
        return ShortcutManagerCompat.requestPinShortcut(ctx, info, null)
    }
}
