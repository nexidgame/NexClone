package com.nex.clone

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nex.clone.core.CloneStore
import com.nex.clone.core.LaunchHelper
import com.nex.clone.core.ProfileOpClient

/** Trampolin tanpa tampilan: dipakai pintasan desktop, menghormati kunci aplikasi. */
class LaunchCloneActivity : AppCompatActivity() {

    private val ops = ProfileOpClient(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PKG)
        if (pkg == null || CloneStore.get(pkg) == null) {
            Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        LaunchHelper.launch(this, ops, pkg) { finish() }
    }

    companion object {
        const val EXTRA_PKG = "pkg"
    }
}
