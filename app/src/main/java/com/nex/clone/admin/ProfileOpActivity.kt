package com.nex.clone.admin

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.nex.clone.core.ProfileOps
import com.nex.clone.core.ProfileSetup

/**
 * Berjalan HANYA di profil klon (diaktifkan oleh ProfileSetup). Menerima perintah dari Nex Clone
 * di profil utama dan mengeksekusinya sebagai pemilik profil (profile owner).
 */
class ProfileOpActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var pendingDelete: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val op = intent.getStringExtra(ProfileOps.EXTRA_OP)
        val pkgs = intent.getStringArrayExtra(ProfileOps.EXTRA_PKGS)?.toList() ?: emptyList()
        val dpm = getSystemService(DevicePolicyManager::class.java)
        val admin = ProfileSetup.admin(this)

        if (op == null || !dpm.isProfileOwnerApp(packageName)) {
            finishWith(false)
            return
        }
        when (op) {
            ProfileOps.OP_PING -> finishWith(true)
            ProfileOps.OP_CLONE -> finishWith(pkgs.isNotEmpty() && pkgs.all { cloneOne(dpm, admin, it) })
            ProfileOps.OP_REPAIR -> finishWith(pkgs.isNotEmpty() && pkgs.all { repairOne(dpm, admin, it) })
            ProfileOps.OP_DELETE -> if (pkgs.isEmpty()) finishWith(false) else startDelete(dpm, admin, pkgs.first())
            ProfileOps.OP_STOP -> stopAll(dpm, admin, pkgs)
            ProfileOps.OP_LAUNCH -> finishWith(pkgs.isNotEmpty() && launch(pkgs.first()))
            ProfileOps.OP_NOTIF -> finishWith(pkgs.isNotEmpty() && openNotificationSettings(pkgs.first()))
            else -> finishWith(false)
        }
    }

    private fun isInstalledHere(pkg: String): Boolean =
        runCatching { packageManager.getPackageInfo(pkg, 0); true }.getOrDefault(false)

    private fun cloneOne(dpm: DevicePolicyManager, admin: ComponentName, pkg: String): Boolean {
        var ok = false
        runCatching { ok = dpm.installExistingPackage(admin, pkg) }
        runCatching { dpm.setApplicationHidden(admin, pkg, false) }
        runCatching { dpm.setPackagesSuspended(admin, arrayOf(pkg), false) }
        return ok || isInstalledHere(pkg)
    }

    /** "Perbaiki satu kunci": pasang ulang, tampilkan kembali, dan lepas jeda/blokir. */
    private fun repairOne(dpm: DevicePolicyManager, admin: ComponentName, pkg: String): Boolean {
        runCatching { dpm.setUninstallBlocked(admin, pkg, false) }
        runCatching { dpm.enableSystemApp(admin, pkg) }
        return cloneOne(dpm, admin, pkg)
    }

    private fun startDelete(dpm: DevicePolicyManager, admin: ComponentName, pkg: String) {
        if (!isInstalledHere(pkg)) {
            finishWith(true)
            return
        }
        pendingDelete = pkg
        try {
            startActivityForResult(
                Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).putExtra(Intent.EXTRA_RETURN_RESULT, true),
                REQ_DELETE,
            )
        } catch (e: Exception) {
            // Cadangan: sembunyikan dari profil klon.
            val hidden = runCatching { dpm.setApplicationHidden(admin, pkg, true) }.getOrDefault(false)
            finishWith(hidden)
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_DELETE) {
            val pkg = pendingDelete
            finishWith(pkg == null || !isInstalledHere(pkg))
        }
    }

    /** Menghentikan proses aplikasi klon: jeda sebentar (memaksa berhenti) lalu aktifkan lagi. */
    private fun stopAll(dpm: DevicePolicyManager, admin: ComponentName, pkgs: List<String>) {
        if (pkgs.isEmpty()) {
            finishWith(false)
            return
        }
        val arr = pkgs.toTypedArray()
        runCatching { dpm.setPackagesSuspended(admin, arr, true) }
        val am = getSystemService(ActivityManager::class.java)
        pkgs.forEach { runCatching { am.killBackgroundProcesses(it) } }
        handler.postDelayed({
            runCatching { dpm.setPackagesSuspended(admin, arr, false) }
            finishWith(true)
        }, 500)
    }

    private fun launch(pkg: String): Boolean {
        val i = packageManager.getLaunchIntentForPackage(pkg) ?: return false
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { startActivity(i) }.isSuccess
    }

    private fun openNotificationSettings(pkg: String): Boolean {
        val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { startActivity(i) }.isSuccess
    }

    private fun finishWith(ok: Boolean) {
        setResult(RESULT_OK, Intent().putExtra(ProfileOps.EXTRA_OK, ok))
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val REQ_DELETE = 17
    }
}
