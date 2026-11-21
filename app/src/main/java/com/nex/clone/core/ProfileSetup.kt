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
import com.nex.clone.admin.ProfileHomeActivity
import com.nex.clone.admin.ProfileOpActivity

/** Penyiapan sisi profil kerja (dijalankan oleh instance Nex Clone yang ada di dalam profil kerja). */
object ProfileSetup {

    private val BASE_APPS = listOf(
        "com.google.android.gms", "com.google.android.gsf",
        "com.android.vending", "com.google.android.webview",
    )

    fun admin(ctx: Context) = ComponentName(ctx, NexAdminReceiver::class.java)

    /**
     * Idempotent. Selalu mengatur status komponen; sisanya hanya bila aplikasi ini pengelola profil.
     * @return true bila penyiapan sebagai pengelola profil dijalankan.
     */
    fun run(ctx: Context): Boolean {
        applyComponentStates(ctx)
        val dpm = ctx.getSystemService(DevicePolicyManager::class.java)
        if (!dpm.isProfileOwnerApp(ctx.packageName)) return false
        val admin = admin(ctx)

        runCatching { dpm.setProfileName(admin, ctx.getString(R.string.app_name)) }
        runCatching { dpm.setProfileEnabled(admin) }
        runCatching {
            // Izinkan Nex Clone di profil utama mengirim perintah ke Nex Clone di profil ini.
            // Nama flag membingungkan, jadi kedua arah dipasang sekaligus (seperti TestDPC/Shelter).
            val f = IntentFilter(ProfileOps.ACTION).apply { addCategory(Intent.CATEGORY_DEFAULT) }
            dpm.clearCrossProfileIntentFilters(admin)
            dpm.addCrossProfileIntentFilter(
                admin, f,
                DevicePolicyManager.FLAG_PARENT_CAN_ACCESS_MANAGED or DevicePolicyManager.FLAG_MANAGED_CAN_ACCESS_PARENT,
            )
        }
        // Layanan Google dasar supaya aplikasi klon (mis. yang butuh notifikasi push) bisa berjalan.
        for (p in BASE_APPS) runCatching { dpm.enableSystemApp(admin, p) }
        return true
    }

    /**
     * Di dalam profil kerja: aktifkan penerima perintah dan layar "Perbaiki/Hapus profil kerja",
     * sembunyikan layar utama klon (yang hanya dipakai di profil utama).
     */
    fun applyComponentStates(ctx: Context) {
        val pm = ctx.packageManager
        fun set(cls: Class<*>, enabled: Boolean) = runCatching {
            pm.setComponentEnabledSetting(
                ComponentName(ctx, cls),
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
        set(ProfileOpActivity::class.java, true)
        set(ProfileHomeActivity::class.java, true)
        set(MainActivity::class.java, false)
    }
}
