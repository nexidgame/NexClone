package com.nex.clone.core

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/** Protokol perintah dari Nex Clone (profil utama) ke Nex Clone di profil klon. */
object ProfileOps {
    const val ACTION = "com.nex.clone.action.PROFILE_OP"
    const val EXTRA_OP = "op"
    const val EXTRA_PKGS = "pkgs"
    const val EXTRA_OK = "ok"

    const val OP_PING = "ping"
    const val OP_CLONE = "clone"
    const val OP_REPAIR = "repair"
    const val OP_DELETE = "delete"
    const val OP_STOP = "stop"
    const val OP_LAUNCH = "launch"
    const val OP_NOTIF = "notif"
}

/**
 * Mengirim perintah ke profil klon lewat cross-profile intent. Harus dibuat sebagai properti Activity
 * (supaya terdaftar sebelum STARTED). Intent TIDAK boleh diberi package/komponen — itu mematikan
 * penerusan lintas profil.
 */
class ProfileOpClient(activity: ComponentActivity) {
    private var callback: ((Boolean) -> Unit)? = null

    private val launcher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val cb = callback
        callback = null
        val ok = r.resultCode == Activity.RESULT_OK && (r.data?.getBooleanExtra(ProfileOps.EXTRA_OK, false) ?: false)
        cb?.invoke(ok)
    }

    fun run(op: String, pkgs: List<String>, onDone: (Boolean) -> Unit) {
        val i = Intent(ProfileOps.ACTION)
            .putExtra(ProfileOps.EXTRA_OP, op)
            .putExtra(ProfileOps.EXTRA_PKGS, pkgs.toTypedArray())
        callback = onDone
        try {
            launcher.launch(i)
        } catch (e: ActivityNotFoundException) {
            callback = null
            onDone(false)
        } catch (e: SecurityException) {
            callback = null
            onDone(false)
        }
    }
}
