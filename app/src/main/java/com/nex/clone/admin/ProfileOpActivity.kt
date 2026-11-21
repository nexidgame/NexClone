package com.nex.clone.admin

import android.app.Activity
import android.app.ActivityManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserManager
import android.provider.Settings
import androidx.core.content.IntentCompat
import com.nex.clone.core.ProfileOps
import com.nex.clone.core.ProfileSetup
import java.io.File
import java.io.FileInputStream

/**
 * Berjalan HANYA di profil kerja (diaktifkan oleh ProfileSetup). Menerima perintah dari Nex Clone
 * di profil utama dan mengeksekusinya sebagai pemilik profil (profile owner).
 *
 * Instal aplikasi ke profil kerja (mirip "Clone" di Island), bertingkat:
 *  1. installExistingPackage  — memasang salinan dari paket yang sudah ada di ponsel (tanpa unduh)
 *  2. enableSystemApp         — untuk aplikasi sistem
 *  3. lepas pembatasan instal lalu ulangi langkah 1
 *  4. PackageInstaller        — memasang dari APK sumber (dengan layar konfirmasi sistem)
 *  5. Play Store profil kerja — dibuka manual sebagai pilihan terakhir
 */
class ProfileOpActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var pendingDelete: String? = null
    private val log = StringBuilder()
    private var installPkg: String? = null
    private var finished = false

    private fun note(s: String) {
        if (log.isNotEmpty()) log.append("; ")
        log.append(s)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val op = intent.getStringExtra(ProfileOps.EXTRA_OP)
        val pkgs = intent.getStringArrayExtra(ProfileOps.EXTRA_PKGS)?.toList() ?: emptyList()
        val apks = intent.getStringArrayExtra(ProfileOps.EXTRA_APKS)?.toList() ?: emptyList()
        val dpm = getSystemService(DevicePolicyManager::class.java)
        val admin = ProfileSetup.admin(this)

        if (op == null) {
            finishWith(false, "no op")
            return
        }
        if (!dpm.isProfileOwnerApp(packageName)) {
            finishWith(false, "not profile owner")
            return
        }
        when (op) {
            ProfileOps.OP_PING -> finishWith(true)
            ProfileOps.OP_CLONE -> if (pkgs.isEmpty()) finishWith(false, "no package") else install(dpm, admin, pkgs.first(), apks)
            ProfileOps.OP_REPAIR -> if (pkgs.isEmpty()) finishWith(false, "no package") else {
                runCatching { dpm.setUninstallBlocked(admin, pkgs.first(), false) }
                install(dpm, admin, pkgs.first(), apks)
            }
            ProfileOps.OP_DELETE -> if (pkgs.isEmpty()) finishWith(false, "no package") else startDelete(dpm, admin, pkgs.first())
            ProfileOps.OP_STOP -> stopAll(dpm, admin, pkgs)
            ProfileOps.OP_LAUNCH -> finishWith(pkgs.isNotEmpty() && launch(pkgs.first()))
            ProfileOps.OP_NOTIF -> finishWith(pkgs.isNotEmpty() && openNotificationSettings(pkgs.first()))
            else -> finishWith(false, "unknown op")
        }
    }

    private fun isInstalledHere(pkg: String): Boolean = runCatching {
        val info = packageManager.getApplicationInfo(pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        (info.flags and ApplicationInfo.FLAG_INSTALLED) != 0
    }.getOrDefault(false)

    private fun unhide(dpm: DevicePolicyManager, admin: ComponentName, pkg: String) {
        runCatching { dpm.setApplicationHidden(admin, pkg, false) }
        runCatching { dpm.setPackagesSuspended(admin, arrayOf(pkg), false) }
    }

    // ------------------------------------------------------------------ instal bertingkat

    private fun tryInstallExisting(dpm: DevicePolicyManager, admin: ComponentName, pkg: String, label: String): Boolean {
        return try {
            val r = dpm.installExistingPackage(admin, pkg)
            note("$label=$r")
            r || isInstalledHere(pkg)
        } catch (e: Throwable) {
            note("$label: ${e.javaClass.simpleName}: ${e.message}")
            isInstalledHere(pkg)
        }
    }

    private fun install(dpm: DevicePolicyManager, admin: ComponentName, pkg: String, apks: List<String>) {
        installPkg = pkg
        if (isInstalledHere(pkg)) {
            unhide(dpm, admin, pkg)
            finishWith(true)
            return
        }
        // 1. paket yang sudah ada di ponsel
        if (tryInstallExisting(dpm, admin, pkg, "installExisting")) {
            unhide(dpm, admin, pkg)
            finishWith(true)
            return
        }
        // 2. aplikasi sistem
        try {
            dpm.enableSystemApp(admin, pkg)
            note("enableSystemApp ok")
        } catch (e: Throwable) {
            note("enableSystemApp: ${e.javaClass.simpleName}")
        }
        if (isInstalledHere(pkg)) {
            unhide(dpm, admin, pkg)
            finishWith(true)
            return
        }
        // 3. lepas pembatasan instal yang mungkin menghalangi, lalu ulangi
        for (r in listOf(UserManager.DISALLOW_INSTALL_APPS, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)) {
            runCatching { dpm.clearUserRestriction(admin, r) }
        }
        if (tryInstallExisting(dpm, admin, pkg, "retry")) {
            unhide(dpm, admin, pkg)
            finishWith(true)
            return
        }
        // 4. pasang dari APK sumber lewat PackageInstaller (butuh konfirmasi pengguna)
        if (apks.isNotEmpty()) {
            installFromApks(pkg, apks)
        } else {
            note("no source apk")
            openStore(pkg)
        }
    }

    private fun installFromApks(pkg: String, apks: List<String>) {
        try {
            val installer = packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            params.setAppPackageName(pkg)
            val sid = installer.createSession(params)
            installer.openSession(sid).use { session ->
                apks.forEachIndexed { i, path ->
                    val f = File(path)
                    FileInputStream(f).use { input ->
                        session.openWrite("apk_$i.apk", 0, f.length()).use { out ->
                            input.copyTo(out)
                            session.fsync(out)
                        }
                    }
                }
                InstallBridge.onStatus = { onInstallStatus(it) }
                val statusIntent = Intent(this, InstallStatusReceiver::class.java)
                val pending = PendingIntent.getBroadcast(
                    this, sid, statusIntent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                session.commit(pending.intentSender)
            }
            // Beri waktu pengguna untuk mengonfirmasi di layar sistem.
            handler.postDelayed({ if (!finished) finishWith(false, "install timeout; $log") }, 5 * 60_000L)
        } catch (e: Throwable) {
            note("PackageInstaller: ${e.javaClass.simpleName}: ${e.message}")
            openStore(pkg)
        }
    }

    private fun onInstallStatus(i: Intent) {
        when (val status = i.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = IntentCompat.getParcelableExtra(i, Intent.EXTRA_INTENT, Intent::class.java)
                if (confirm != null) {
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { startActivity(confirm) }
                        .onFailure { note("confirm: ${it.javaClass.simpleName}") }
                }
            }
            PackageInstaller.STATUS_SUCCESS -> finishWith(true)
            else -> {
                note("install status=$status ${i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()}")
                openStore(installPkg.orEmpty())
            }
        }
    }

    /** Pilihan terakhir: buka Play Store di profil kerja agar pengguna menginstal manual. */
    private fun openStore(pkg: String) {
        if (pkg.isEmpty()) {
            finishWith(false, log.toString())
            return
        }
        val i = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
            .setPackage("com.android.vending")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val opened = runCatching { startActivity(i) }.isSuccess
        finishWith(false, if (opened) "${ProfileOps.REASON_OPENED_STORE}; $log" else log.toString())
    }

    // ------------------------------------------------------------------ hapus / hentikan / buka

    private fun startDelete(dpm: DevicePolicyManager, admin: ComponentName, pkg: String) {
        if (!isInstalledHere(pkg)) {
            finishWith(true)
            return
        }
        val isSystem = runCatching {
            (packageManager.getApplicationInfo(pkg, 0).flags and ApplicationInfo.FLAG_SYSTEM) != 0
        }.getOrDefault(false)
        if (isSystem) {
            // Aplikasi sistem (mis. Chrome, Play Store) tidak bisa di-uninstall: cukup disembunyikan dari profil.
            finishWith(runCatching { dpm.setApplicationHidden(admin, pkg, true) }.getOrDefault(false))
            return
        }
        pendingDelete = pkg
        try {
            startActivityForResult(
                Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).putExtra(Intent.EXTRA_RETURN_RESULT, true),
                REQ_DELETE,
            )
        } catch (e: Exception) {
            // Cadangan: sembunyikan dari profil kerja.
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

    /** Menghentikan proses aplikasi: jeda sebentar (memaksa berhenti) lalu aktifkan lagi. */
    private fun stopAll(dpm: DevicePolicyManager, admin: ComponentName, pkgs: List<String>) {
        if (pkgs.isEmpty()) {
            finishWith(false, "no package")
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

    private fun finishWith(ok: Boolean, reason: String? = null) {
        if (finished) return
        finished = true
        InstallBridge.onStatus = null
        val result = Intent().putExtra(ProfileOps.EXTRA_OK, ok)
        val why = reason ?: if (!ok && log.isNotEmpty()) log.toString() else null
        if (why != null) result.putExtra(ProfileOps.EXTRA_REASON, why)
        setResult(RESULT_OK, result)
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (!finished) InstallBridge.onStatus = null
        super.onDestroy()
    }

    companion object {
        private const val REQ_DELETE = 17
    }
}

/** Menerima status sesi PackageInstaller dan meneruskannya ke ProfileOpActivity (proses yang sama). */
class InstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        InstallBridge.onStatus?.invoke(intent)
    }
}

object InstallBridge {
    @Volatile
    var onStatus: ((Intent) -> Unit)? = null
}
