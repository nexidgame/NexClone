package com.nex.clone

import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nex.clone.core.CloneItem
import com.nex.clone.core.CloneItems
import com.nex.clone.core.CloneStore
import com.nex.clone.core.IconUtils
import com.nex.clone.core.LaunchHelper
import com.nex.clone.core.ProfileOpClient
import com.nex.clone.core.ProfileOps
import com.nex.clone.core.ProfileSetup
import com.nex.clone.core.Profiles
import com.nex.clone.core.Shortcuts
import com.nex.clone.core.Support
import com.nex.clone.core.message
import com.nex.clone.databinding.ActivityMainBinding
import com.nex.clone.ui.Busy
import com.nex.clone.ui.applySystemBarsPadding
import com.nex.clone.ui.dp

/**
 * Beranda: semua aplikasi di ponsel (bawaan). Yang sudah ada di profil kerja diberi lencana koper.
 * Ketuk = buka (salinan profil kerja bila ada, kalau tidak aplikasi aslinya). Tahan = menu.
 * Saat pertama kali dibuka, profil kerja langsung disiapkan.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private val ops = ProfileOpClient(this)
    private val handler = Handler(Looper.getMainLooper())

    private val adapter = CloneGridAdapter(
        onClick = { open(it) },
        onLong = { item, anchor -> showMenu(item, anchor) },
    )

    private var all: List<CloneItem> = emptyList()
    private var query = ""
    private var searching = false
    private var profileReady = false

    private val reload = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh() }

    private val provision = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == RESULT_OK) {
            waitForProfile()
        } else {
            Toast.makeText(this, R.string.setup_cancelled, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.content.applySystemBarsPadding()

        b.list.layoutManager = GridLayoutManager(this, 4)
        b.list.adapter = adapter

        b.btnSearch.setOnClickListener { setSearching(true) }
        b.btnSearchBack.setOnClickListener { setSearching(false) }
        b.btnSearchClear.setOnClickListener { b.etSearch.setText("") }
        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString().orEmpty()
                render()
            }
        })

        b.btnMenu.setOnClickListener { startActivity(Intent(this, UserCenterActivity::class.java)) }
        b.fab.setOnClickListener {
            if (profileReady) reload.launch(Intent(this, PickAppActivity::class.java)) else confirmSetup()
        }
        b.btnSetup.setOnClickListener { startSetup() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (searching) {
                    setSearching(false)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        if (savedInstanceState == null) b.root.post { maybeAutoSetup() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    /** Pertama kali masuk aplikasi: langsung buat profil kerja (sekali saja; berikutnya lewat tombol). */
    private fun maybeAutoSetup() {
        if (CloneStore.flag(FLAG_AUTO_SETUP)) return
        CloneStore.setFlag(FLAG_AUTO_SETUP, true)
        if (Profiles.cloneUser(this) != null) return
        Toast.makeText(this, R.string.setup_auto, Toast.LENGTH_SHORT).show()
        startSetup()
    }

    // ------------------------------------------------------------------ daftar

    private fun refresh() {
        Thread {
            val ready = Profiles.cloneUser(this) != null
            val items = CloneItems.loadHome(this)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                profileReady = ready
                all = items
                render()
            }
        }.start()
    }

    private fun render() {
        val list = if (query.isBlank()) all else all.filter { it.label.contains(query, ignoreCase = true) }
        adapter.submit(list)
        b.cardSetup.visibility = if (!profileReady) View.VISIBLE else View.GONE
        b.empty.visibility = if (profileReady && all.isEmpty()) View.VISIBLE else View.GONE
        b.list.visibility = if (!profileReady || list.isEmpty()) View.GONE else View.VISIBLE
        b.dotMenu.visibility = if (!profileReady) View.VISIBLE else View.GONE
    }

    private fun setSearching(on: Boolean) {
        searching = on
        b.barNormal.visibility = if (on) View.GONE else View.VISIBLE
        b.barSearch.visibility = if (on) View.VISIBLE else View.GONE
        b.tvSection.visibility = if (on) View.VISIBLE else View.GONE
        val imm = getSystemService(InputMethodManager::class.java)
        if (on) {
            b.etSearch.requestFocus()
            imm.showSoftInput(b.etSearch, InputMethodManager.SHOW_IMPLICIT)
        } else {
            b.etSearch.setText("")
            imm.hideSoftInputFromWindow(b.etSearch.windowToken, 0)
        }
    }

    private fun open(item: CloneItem) {
        if (item.inProfile) {
            LaunchHelper.launch(this, ops, item.entry.pkg)
        } else {
            LaunchHelper.launchOriginal(this, item.entry.pkg)
        }
    }

    // ------------------------------------------------------------------ menu tahan

    private fun showMenu(item: CloneItem, anchor: View) {
        val v = layoutInflater.inflate(R.layout.popup_clone_menu, null)
        val pw = PopupWindow(v, dp(250), ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            elevation = dp(12).toFloat()
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        val pkg = item.entry.pkg

        // Aplikasi yang belum di profil kerja: hanya "Masukkan ke profil kerja".
        v.findViewById<View>(R.id.menuAdd).visibility = if (item.inProfile) View.GONE else View.VISIBLE
        for (id in intArrayOf(R.id.menuBug, R.id.menuShortcut, R.id.menuEdit, R.id.menuRepair, R.id.menuDelete)) {
            v.findViewById<View>(id).visibility = if (item.inProfile) View.VISIBLE else View.GONE
        }

        v.findViewById<View>(R.id.menuAdd).setOnClickListener {
            pw.dismiss()
            addToProfile(item)
        }
        v.findViewById<View>(R.id.menuBug).setOnClickListener {
            pw.dismiss()
            Support.sendBugReport(this, pkg)
        }
        v.findViewById<View>(R.id.menuShortcut).setOnClickListener {
            pw.dismiss()
            pinShortcut(item)
        }
        v.findViewById<View>(R.id.menuEdit).setOnClickListener {
            pw.dismiss()
            reload.launch(
                Intent(this, EditCloneActivity::class.java)
                    .putExtra(EditCloneActivity.EXTRA_PKG, pkg)
                    .putExtra(EditCloneActivity.EXTRA_EDIT, true)
            )
        }
        v.findViewById<View>(R.id.menuRepair).setOnClickListener {
            pw.dismiss()
            repair(pkg)
        }
        v.findViewById<View>(R.id.menuDelete).setOnClickListener {
            pw.dismiss()
            confirmDelete(item)
        }
        pw.showAsDropDown(anchor, dp(8), 0)
    }

    /** Memasukkan aplikasi ke profil kerja langsung dari beranda. */
    private fun addToProfile(item: CloneItem) {
        if (!profileReady) {
            Toast.makeText(this, R.string.need_profile, Toast.LENGTH_LONG).show()
            confirmSetup()
            return
        }
        val pkg = item.entry.pkg
        val busy = Busy.show(this, R.string.cloning)
        ops.run(ProfileOps.OP_CLONE, listOf(pkg)) { r ->
            busy.dismiss()
            if (r.isOk) {
                CloneStore.upsert(item.entry)
                CloneStore.addHistory(pkg)
                Toast.makeText(this, R.string.clone_done, Toast.LENGTH_SHORT).show()
                refresh()
            } else {
                Toast.makeText(this, r.message(this, R.string.clone_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun pinShortcut(item: CloneItem) {
        val bmp = item.icon?.toBitmap(192, 192)
        val ok = bmp != null && Shortcuts.pin(this, item.entry.pkg, item.label, bmp)
        if (!ok) Toast.makeText(this, R.string.shortcut_failed, Toast.LENGTH_LONG).show()
    }

    private fun repair(pkg: String) {
        val busy = Busy.show(this, R.string.repairing)
        ops.run(ProfileOps.OP_REPAIR, listOf(pkg)) { r ->
            busy.dismiss()
            val msg = if (r.isOk) getString(R.string.repair_done) else r.message(this, R.string.repair_failed)
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            refresh()
        }
    }

    private fun confirmDelete(item: CloneItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle(item.label)
            .setMessage(R.string.delete_confirm)
            .setPositiveButton(R.string.menu_delete) { _, _ -> delete(item) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun delete(item: CloneItem) {
        val busy = Busy.show(this, R.string.deleting)
        ops.run(ProfileOps.OP_DELETE, listOf(item.entry.pkg)) { r ->
            busy.dismiss()
            if (r.isOk) {
                IconUtils.delete(this, item.entry.iconFile)
                CloneStore.remove(item.entry.pkg)
                Toast.makeText(this, R.string.delete_done, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, r.message(this, R.string.delete_failed), Toast.LENGTH_LONG).show()
            }
            refresh()
        }
    }

    // ------------------------------------------------------------------ penyiapan profil kerja

    private fun confirmSetup() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.setup_title)
            .setMessage(R.string.setup_explain)
            .setPositiveButton(R.string.setup_continue) { _, _ -> startSetup() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun startSetup() {
        val dpm = getSystemService(DevicePolicyManager::class.java)
        if (!dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.setup_not_allowed_title)
                .setMessage(R.string.setup_not_allowed)
                .setPositiveButton(R.string.ok, null)
                .show()
            return
        }
        val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)
            .putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, ProfileSetup.admin(this))
        try {
            provision.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.setup_unsupported, Toast.LENGTH_LONG).show()
        }
    }

    private fun waitForProfile() {
        val busy = Busy.show(this, R.string.setup_wait)
        val start = SystemClock.elapsedRealtime()
        lateinit var tick: Runnable
        tick = Runnable {
            Thread {
                val ready = Profiles.cloneUser(this) != null
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    when {
                        ready -> pingUntilReady(busy, 0)
                        SystemClock.elapsedRealtime() - start > 90_000 -> {
                            busy.dismiss()
                            Toast.makeText(this, R.string.setup_timeout, Toast.LENGTH_LONG).show()
                        }
                        else -> handler.postDelayed(tick, 1500)
                    }
                }
            }.start()
        }
        handler.post(tick)
    }

    /** Memastikan penerima perintah di profil kerja sudah aktif; mencoba sampai ~50 detik. */
    private fun pingUntilReady(busy: AlertDialog, attempt: Int) {
        ops.run(ProfileOps.OP_PING, emptyList()) { r ->
            when {
                r.isOk -> {
                    busy.dismiss()
                    Toast.makeText(this, R.string.setup_done, Toast.LENGTH_LONG).show()
                    refresh()
                }
                attempt < 25 -> handler.postDelayed({ pingUntilReady(busy, attempt + 1) }, 2000)
                else -> {
                    busy.dismiss()
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.setup_incomplete_title)
                        .setMessage(getString(R.string.setup_incomplete) + "\n\n(" + r.name + ")")
                        .setPositiveButton(R.string.ok, null)
                        .show()
                    refresh()
                }
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val FLAG_AUTO_SETUP = "auto_setup_asked"
    }
}
