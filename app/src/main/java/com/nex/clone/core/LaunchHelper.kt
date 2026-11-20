package com.nex.clone.core

import android.content.pm.LauncherApps
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.nex.clone.R

/** Menjalankan aplikasi klon di profil klon (dengan kunci aplikasi bila aktif). */
object LaunchHelper {

    fun launch(a: FragmentActivity, ops: ProfileOpClient, pkg: String, onDone: () -> Unit = {}) {
        val entry = CloneStore.get(pkg)
        if (entry?.locked == true) {
            if (!Auth.available(a)) {
                Toast.makeText(a, R.string.auth_unavailable, Toast.LENGTH_LONG).show()
                onDone()
                return
            }
            Auth.prompt(a, a.getString(R.string.auth_title), onOk = { doLaunch(a, ops, pkg, onDone) }, onFail = onDone)
        } else {
            doLaunch(a, ops, pkg, onDone)
        }
    }

    private fun doLaunch(a: FragmentActivity, ops: ProfileOpClient, pkg: String, onDone: () -> Unit) {
        val user = Profiles.cloneUser(a)
        val info = user?.let { u -> runCatching { Profiles.resolve(a, u, pkg) }.getOrNull() }
        if (user != null && info != null) {
            val ok = runCatching {
                a.getSystemService(LauncherApps::class.java).startMainActivity(info.componentName, user, null, null)
            }.isSuccess
            if (ok) {
                onDone()
                return
            }
        }
        // Cadangan: minta instance di profil klon membuka aplikasinya sendiri.
        ops.run(ProfileOps.OP_LAUNCH, listOf(pkg)) { ok ->
            if (!ok) Toast.makeText(a, R.string.launch_failed, Toast.LENGTH_LONG).show()
            onDone()
        }
    }
}
