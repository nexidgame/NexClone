package com.nex.clone.core

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.nex.clone.MainActivity
import com.nex.clone.R
import com.nex.clone.admin.NexAdminReceiver
import com.nex.clone.admin.ProfileOpActivity

/** Penyiapan sisi profil klon (dijalankan oleh instance Nex Clone yang ada di dalam profil kerja). */
object ProfileSetup {

    private val BASE_APPS = listOf(
        "com.google.android.gms", "com.google.android.gsf",
        "com.android.vending", "com.google.android.webview",
    )

    fun admin(ctx: Context) = ComponentName(ctx, NexAdminReceiver::class.java)

    fun run(ctx: Context) {
        val dpm = ctx.getSystemService(DevicePolicyManager::class.java)
        if (!dpm.isProfileOwnerApp(ctx.packageName)) return
        val admin = admin(ctx)

        runCatching { dpm.setProfileName(admin, ctx.getString(R.string.app_name)) }
        runCatching { dpm.setProfileEnabled(admin) }
        runCatching {
            // Izinkan Nex Clone di profil utama mengirim perintah ke Nex Clone di profil ini.
            val f = IntentFilter(ProfileOps.ACTION).apply { addCategory(Intent.CATEGORY_DEFAULT) }
            dpm.clearCrossProfileIntentFilters(admin)
            dpm.addCrossProfileIntentFilter(admin, f, DevicePolicyManager.FLAG_PARENT_CAN_ACCESS_MANAGED)
        }
        // Layanan Google dasar supaya aplikasi klon (mis. yang butuh notifikasi push) bisa berjalan.
        for (p in BASE_APPS) runCatching { dpm.enableSystemApp(admin, p) }
        applyComponentStates(ctx)
    }

    /** Aktifkan penerima perintah & sembunyikan layar utama — hanya berlaku di dalam profil klon. */
    fun applyComponentStates(ctx: Context) {
        val pm = ctx.packageManager
        runCatching {
            pm.setComponentEnabledSetting(
                ComponentName(ctx, ProfileOpActivity::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP,
            )
        }
        runCatching {
            pm.setComponentEnabledSetting(
                ComponentName(ctx, MainActivity::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP,
            )
        }
    }
}
