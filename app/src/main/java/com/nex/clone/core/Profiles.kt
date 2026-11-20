package com.nex.clone.core

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.os.UserManager

/** Akses ke profil klon (work profile) yang dibuat Nex Clone. */
object Profiles {

    /** True bila kode ini sedang berjalan DI DALAM profil klon. */
    fun inManagedProfile(ctx: Context): Boolean =
        ctx.getSystemService(UserManager::class.java).isManagedProfile

    /** Profil klon (selain profil utama) tempat Nex Clone ikut terpasang; null bila belum disiapkan. */
    fun cloneUser(ctx: Context): UserHandle? = runCatching {
        val la = ctx.getSystemService(LauncherApps::class.java)
        val me = Process.myUserHandle()
        la.profiles.firstOrNull { it != me && la.isPackageEnabled(ctx.packageName, it) }
    }.getOrNull()

    /** Semua aplikasi ber-launcher di profil klon: paket -> info. Null bila tidak bisa dibaca. */
    fun snapshot(ctx: Context, user: UserHandle): Map<String, LauncherActivityInfo>? = runCatching {
        val la = ctx.getSystemService(LauncherApps::class.java)
        val out = LinkedHashMap<String, LauncherActivityInfo>()
        for (i in la.getActivityList(null, user)) out.putIfAbsent(i.applicationInfo.packageName, i)
        out as Map<String, LauncherActivityInfo>
    }.getOrNull()

    fun resolve(ctx: Context, user: UserHandle, pkg: String): LauncherActivityInfo? =
        ctx.getSystemService(LauncherApps::class.java).getActivityList(pkg, user).firstOrNull()
}
