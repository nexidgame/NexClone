package com.nex.clone

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nex.clone.core.CloneItems
import com.nex.clone.core.Profiles
import com.nex.clone.core.Support
import com.nex.clone.databinding.ActivityUserCenterBinding
import com.nex.clone.databinding.RowUserBinding
import com.nex.clone.ui.applySystemBarsPadding

/** "Pusat pengguna": pengelolaan notifikasi, tugas, kunci aplikasi, ruang privasi, bantuan, dan bahasa. */
class UserCenterActivity : AppCompatActivity() {

    private lateinit var b: ActivityUserCenterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityUserCenterBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarsPadding()
        b.toolbar.setNavigationOnClickListener { finish() }

        addRow(b.group1, R.drawable.ic_notifications, R.string.uc_notifications) { manage(ManageActivity.MODE_NOTIF) }
        addRow(b.group1, R.drawable.ic_list, R.string.uc_tasks) { manage(ManageActivity.MODE_TASKS) }

        addRow(b.group2, R.drawable.ic_lock, R.string.uc_lock) { manage(ManageActivity.MODE_LOCK) }
        addRow(b.group2, R.drawable.ic_visibility_off, R.string.uc_private, badge = true) {
            startActivity(Intent(this, PrivateSpaceActivity::class.java))
        }

        addRow(b.group3, R.drawable.ic_help, R.string.uc_faq) { info(InfoActivity.MODE_FAQ) }
        addRow(b.group3, R.drawable.ic_feedback, R.string.uc_feedback) { Support.sendFeedback(this) }
        addRow(b.group3, R.drawable.ic_share, R.string.uc_share) { Support.shareApp(this) }
        addRow(b.group3, R.drawable.ic_language, R.string.uc_language, value = languageName()) { chooseLanguage() }
        addRow(b.group3, R.drawable.ic_info, R.string.uc_about) { info(InfoActivity.MODE_ABOUT) }
    }

    override fun onResume() {
        super.onResume()
        val ready = Profiles.cloneUser(this) != null
        b.tvStatusTitle.setText(if (ready) R.string.uc_status_ready else R.string.uc_status_not_ready)
        if (!ready) {
            b.tvStatusDesc.text = getString(R.string.uc_status_hint)
            return
        }
        // Hitung dari isi profil kerja yang sebenarnya (catatan yang sudah dihapus ikut dibersihkan).
        Thread {
            val n = CloneItems.load(this, false).size + CloneItems.load(this, true).size
            runOnUiThread {
                if (!isDestroyed) b.tvStatusDesc.text = getString(R.string.uc_status_count, n)
            }
        }.start()
    }

    private fun addRow(
        group: LinearLayout, icon: Int, title: Int,
        value: String? = null, badge: Boolean = false, onClick: () -> Unit,
    ) {
        val r = RowUserBinding.inflate(layoutInflater, group, false)
        r.rowIcon.setImageResource(icon)
        r.rowTitle.setText(title)
        if (value != null) {
            r.rowValue.text = value
            r.rowValue.visibility = View.VISIBLE
        }
        r.rowBadge.visibility = if (badge) View.VISIBLE else View.GONE
        r.root.setOnClickListener { onClick() }
        group.addView(r.root)
    }

    private fun manage(mode: String) {
        startActivity(Intent(this, ManageActivity::class.java).putExtra(ManageActivity.EXTRA_MODE, mode))
    }

    private fun info(mode: String) {
        startActivity(Intent(this, InfoActivity::class.java).putExtra(InfoActivity.EXTRA_MODE, mode))
    }

    private fun languageName(): String {
        val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore(',').substringBefore('-')
        return when (tag) {
            "id" -> getString(R.string.lang_id)
            "en" -> getString(R.string.lang_en)
            else -> getString(R.string.lang_system)
        }
    }

    private fun chooseLanguage() {
        val names = arrayOf(getString(R.string.lang_system), getString(R.string.lang_id), getString(R.string.lang_en))
        val tags = arrayOf("", "id", "en")
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.uc_language)
            .setItems(names) { _, which ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags[which]))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
