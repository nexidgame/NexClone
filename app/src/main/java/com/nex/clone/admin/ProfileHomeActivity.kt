package com.nex.clone.admin

import android.app.admin.DevicePolicyManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nex.clone.R
import com.nex.clone.core.ProfileSetup
import com.nex.clone.databinding.ActivityProfileHomeBinding
import com.nex.clone.ui.applySystemBarsPadding

/**
 * Layar Nex Clone DI DALAM profil kerja (diaktifkan hanya di sana). Hanya dua menu:
 * Perbaiki (jalankan ulang penyiapan) dan Hapus profil kerja.
 */
class ProfileHomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityProfileHomeBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarsPadding()

        b.btnRepair.setOnClickListener {
            val ok = ProfileSetup.run(this)
            Toast.makeText(this, if (ok) R.string.ph_repair_done else R.string.ph_repair_not_owner, Toast.LENGTH_LONG).show()
        }
        b.btnDelete.setOnClickListener { confirmDelete() }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.ph_delete)
            .setMessage(R.string.ph_delete_confirm)
            .setPositiveButton(R.string.ph_delete_yes) { _, _ -> deleteProfile() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Pemilik profil memanggil wipeData: menghapus profil kerja beserta semua klon dan datanya. */
    private fun deleteProfile() {
        val dpm = getSystemService(DevicePolicyManager::class.java)
        if (!dpm.isProfileOwnerApp(packageName)) {
            Toast.makeText(this, R.string.ph_repair_not_owner, Toast.LENGTH_LONG).show()
            return
        }
        runCatching { dpm.wipeData(0) }
            .onFailure { Toast.makeText(this, R.string.ph_delete_failed, Toast.LENGTH_LONG).show() }
    }
}
