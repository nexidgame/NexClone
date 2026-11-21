package com.nex.clone.core

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/** Item tampilan klon (nama + ikon yang sudah diputuskan). */
data class CloneItem(
    val entry: CloneEntry,
    val label: String,
    val icon: Drawable?,
    val missing: Boolean,
    /** True bila aplikasi ini sudah ada di profil kerja. */
    val inProfile: Boolean = true,
)

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

    /**
     * Beranda: SEMUA aplikasi di ponsel (bawaan), ditandai bila sudah ada di profil kerja.
     * Aplikasi yang disembunyikan di Ruang Privasi tidak ditampilkan. Boleh dipanggil dari thread latar.
     */
    fun loadHome(ctx: Context): List<CloneItem> {
        val pm = ctx.packageManager
        val user = Profiles.cloneUser(ctx)
        val snap = user?.let { Profiles.snapshot(ctx, it) }
        val store = CloneStore.all().associateBy { it.pkg }
        val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val seen = HashSet<String>()
        val out = ArrayList<CloneItem>()
        for (ri in pm.queryIntentActivities(launch, 0)) {
            val pkg = ri.activityInfo.packageName
            if (pkg == ctx.packageName || !seen.add(pkg)) continue
            val saved = store[pkg]
            if (saved?.priv == true) continue
            val inProfile = snap?.containsKey(pkg) == true
            val entry = saved ?: CloneEntry(pkg)
            val custom = if (inProfile) {
                IconUtils.loadCustom(ctx, entry.iconFile)?.let { BitmapDrawable(ctx.resources, it) }
            } else null
            val icon: Drawable? = custom ?: runCatching { ri.loadIcon(pm) }.getOrNull()
            val label = (if (inProfile) saved?.name else null) ?: ri.loadLabel(pm).toString()
            out.add(CloneItem(entry, label, icon, missing = false, inProfile = inProfile))
        }
        return out.sortedBy { it.label.lowercase() }
    }
}
