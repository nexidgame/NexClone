package com.nex.clone

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nex.clone.core.Auth
import com.nex.clone.core.CloneItem
import com.nex.clone.core.CloneItems
import com.nex.clone.core.CloneStore
import com.nex.clone.core.LaunchHelper
import com.nex.clone.core.ProfileOpClient
import com.nex.clone.databinding.ActivityPrivateBinding
import com.nex.clone.ui.applySystemBarsPadding

/** "Ruang Privasi": klon yang disembunyikan dari daftar utama, dibuka dengan verifikasi layar kunci. */
class PrivateSpaceActivity : AppCompatActivity() {

    private lateinit var b: ActivityPrivateBinding
    private val ops = ProfileOpClient(this)
    private var unlocked = false
    private val adapter = CloneGridAdapter(
        onClick = { LaunchHelper.launch(this, ops, it.entry.pkg) },
        onLong = { item, _ -> confirmRemove(item) },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPrivateBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarsPadding()
        b.toolbar.setNavigationOnClickListener { finish() }
        b.list.layoutManager = GridLayoutManager(this, 4)
        b.list.adapter = adapter
        b.fab.setOnClickListener { chooseToMove() }

        if (!Auth.available(this)) {
            Toast.makeText(this, R.string.auth_unavailable, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        Auth.prompt(this, getString(R.string.auth_title), onOk = {
            unlocked = true
            b.content.visibility = View.VISIBLE
            b.fab.visibility = View.VISIBLE
            refresh()
        }, onFail = { finish() })
    }

    override fun onStop() {
        super.onStop()
        // Kunci ulang saat meninggalkan layar ini.
        if (!isChangingConfigurations && unlocked) finish()
    }

    private fun refresh() {
        Thread {
            val list = CloneItems.load(this, true)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                adapter.submit(list)
                b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }.start()
    }

    private fun chooseToMove() {
        Thread {
            val candidates: List<CloneItem> = CloneItems.load(this, false)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                if (candidates.isEmpty()) {
                    Toast.makeText(this, R.string.private_none_to_add, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                val names = candidates.map { it.label }.toTypedArray()
                val checked = BooleanArray(candidates.size)
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.private_add_title)
                    .setMultiChoiceItems(names, checked) { _, which, isChecked -> checked[which] = isChecked }
                    .setPositiveButton(R.string.ok) { _, _ ->
                        candidates.forEachIndexed { i, c ->
                            if (checked[i]) {
                                c.entry.priv = true
                                CloneStore.upsert(c.entry)
                            }
                        }
                        refresh()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }.start()
    }

    private fun confirmRemove(item: CloneItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle(item.label)
            .setMessage(R.string.private_remove_confirm)
            .setPositiveButton(R.string.private_remove) { _, _ ->
                item.entry.priv = false
                CloneStore.upsert(item.entry)
                refresh()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
