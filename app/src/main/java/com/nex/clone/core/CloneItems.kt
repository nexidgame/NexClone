package com.nex.clone.core

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/** Item tampilan klon (nama + ikon yang sudah diputuskan). */
data class CloneItem(val entry: CloneEntry, val label: String, val icon: Drawable?, val missing: Boolean)

object CloneItems {

    /** Boleh dipanggil dari thread latar. [wantPrivate] memilih daftar utama atau Ruang Privasi. */
    fun load(ctx: Context, wantPrivate: Boolean): List<CloneItem> {
        val user = Profiles.cloneUser(ctx)
        val snap = user?.let { Profiles.snapshot(ctx, it) }
        val pm = ctx.packageManager
        return CloneStore.all().filter { it.priv == wantPrivate }.map { e ->
            val info = snap?.get(e.pkg)
            val custom = IconUtils.loadCustom(ctx, e.iconFile)?.let { BitmapDrawable(ctx.resources, it) }
            val icon: Drawable? = custom
                ?: runCatching { info?.getIcon(0) }.getOrNull()
                ?: runCatching { pm.getApplicationIcon(e.pkg) }.getOrNull()
            val label = e.name
                ?: info?.label?.toString()
                ?: runCatching { pm.getApplicationLabel(pm.getApplicationInfo(e.pkg, 0)).toString() }.getOrNull()
                ?: e.pkg
            CloneItem(e, label, icon, snap != null && info == null)
        }
    }
}
