package com.nex.clone.core

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.nex.clone.R

/** Protokol perintah dari Nex Clone (profil utama) ke Nex Clone di profil kerja. */
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

/** Hasil pengiriman perintah ke profil kerja. */
enum class OpResult {
    /** Perintah dijalankan dan berhasil. */
    OK,

    /** Perintah sampai ke profil kerja tetapi gagal dijalankan. */
    FAILED,

    /** Tidak ada penerima di profil kerja (izin lintas profil/penerima belum aktif). */
    NOT_FOUND,

    /** Penerima ada tetapi membatalkan (mis. bukan pengelola profil). */
    CANCELED;

    val isOk: Boolean get() = this == OK
}

/** Pesan untuk pengguna; [failedRes] dipakai bila perintahnya sendiri yang gagal. */
fun OpResult.message(ctx: Context, failedRes: Int): String = when (this) {
    OpResult.NOT_FOUND -> ctx.getString(R.string.op_err_not_found)
    OpResult.CANCELED -> ctx.getString(R.string.op_err_canceled)
    else -> ctx.getString(failedRes)
}

/**
 * Mengirim perintah ke profil kerja lewat cross-profile intent. Harus dibuat sebagai properti Activity
 * (supaya terdaftar sebelum STARTED). Intent TIDAK boleh diberi package/komponen — itu mematikan
 * penerusan lintas profil.
 */
class ProfileOpClient(activity: ComponentActivity) {
    private var callback: ((OpResult) -> Unit)? = null

    private val launcher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val cb = callback
        callback = null
        val result = when {
            r.resultCode != Activity.RESULT_OK -> OpResult.CANCELED
            r.data?.getBooleanExtra(ProfileOps.EXTRA_OK, false) == true -> OpResult.OK
            else -> OpResult.FAILED
        }
        cb?.invoke(result)
    }

    fun run(op: String, pkgs: List<String>, onDone: (OpResult) -> Unit) {
        val i = Intent(ProfileOps.ACTION)
            .putExtra(ProfileOps.EXTRA_OP, op)
            .putExtra(ProfileOps.EXTRA_PKGS, pkgs.toTypedArray())
        callback = onDone
        try {
            launcher.launch(i)
        } catch (e: ActivityNotFoundException) {
            callback = null
            onDone(OpResult.NOT_FOUND)
        } catch (e: SecurityException) {
            callback = null
            onDone(OpResult.CANCELED)
        }
    }
}
