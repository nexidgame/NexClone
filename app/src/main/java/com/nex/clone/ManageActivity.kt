package com.nex.clone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nex.clone.core.Auth
import com.nex.clone.core.CloneItem
import com.nex.clone.core.CloneItems
import com.nex.clone.core.CloneStore
import com.nex.clone.core.ProfileOpClient
import com.nex.clone.core.ProfileOps
import com.nex.clone.databinding.ActivityManageBinding
import com.nex.clone.databinding.ItemManageBinding
import com.nex.clone.ui.applySystemBarsPadding

/** Satu layar dengan tiga mode: Manajemen pemberitahuan, Manajemen tugas, dan Kunci aplikasi. */
class ManageActivity : AppCompatActivity() {

    private lateinit var b: ActivityManageBinding
    private lateinit var mode: String
    private val ops = ProfileOpClient(this)
    private var items: List<CloneItem> = emptyList()
    private val adapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_NOTIF
        b = ActivityManageBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarsPadding()
        b.toolbar.setNavigationOnClickListener { finish() }

        when (mode) {
            MODE_TASKS -> {
                b.toolbar.setTitle(R.string.uc_tasks)
                b.tvHint.setText(R.string.tasks_hint)
                b.btnStopAll.visibility = View.VISIBLE
                b.btnStopAll.setOnClickListener {
                    if (items.isNotEmpty()) stop(items.map { it.entry.pkg })
                }
            }
            MODE_LOCK -> {
                b.toolbar.setTitle(R.string.uc_lock)
                b.tvHint.setText(R.string.lock_hint)
            }
            else -> {
                b.toolbar.setTitle(R.string.uc_notifications)
                b.tvHint.setText(R.string.notif_hint)
            }
        }
        b.list.layoutManager = LinearLayoutManager(this)
        b.list.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        Thread {
            val list = CloneItems.load(this, false)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                items = list
                adapter.notifyDataSetChanged()
                b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }.start()
    }

    private fun stop(pkgs: List<String>) {
        ops.run(ProfileOps.OP_STOP, pkgs) { ok ->
            Toast.makeText(this, if (ok) R.string.tasks_stopped else R.string.op_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openNotificationSettings(pkg: String) {
        ops.run(ProfileOps.OP_NOTIF, listOf(pkg)) { ok ->
            if (!ok) Toast.makeText(this, R.string.op_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.VH>() {

        inner class VH(val vb: ItemManageBinding) : RecyclerView.ViewHolder(vb.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemManageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val vb = holder.vb
            vb.imgIcon.setImageDrawable(item.icon)
            vb.tvLabel.text = item.label
            vb.tvSub.text = item.entry.pkg
            vb.sw.visibility = View.GONE
            vb.btnAction.visibility = View.GONE
            vb.chev.visibility = View.GONE
            vb.root.setOnClickListener(null)

            when (mode) {
                MODE_TASKS -> {
                    vb.btnAction.visibility = View.VISIBLE
                    vb.btnAction.setText(R.string.tasks_stop)
                    vb.btnAction.setOnClickListener { stop(listOf(item.entry.pkg)) }
                }
                MODE_LOCK -> {
                    vb.sw.visibility = View.VISIBLE
                    vb.sw.setOnCheckedChangeListener(null)
                    vb.sw.isChecked = item.entry.locked
                    vb.sw.setOnCheckedChangeListener { btn, checked ->
                        if (btn.isPressed) onLockToggle(item, checked, vb)
                    }
                    vb.root.setOnClickListener { vb.sw.toggle() }
                }
                else -> {
                    vb.chev.visibility = View.VISIBLE
                    vb.root.setOnClickListener { openNotificationSettings(item.entry.pkg) }
                }
            }
        }

        private fun onLockToggle(item: CloneItem, wantLocked: Boolean, vb: ItemManageBinding) {
            fun revert() {
                vb.sw.setOnCheckedChangeListener(null)
                vb.sw.isChecked = item.entry.locked
                vb.sw.setOnCheckedChangeListener { btn, checked ->
                    if (btn.isPressed) onLockToggle(item, checked, vb)
                }
            }
            fun apply() {
                item.entry.locked = wantLocked
                CloneStore.upsert(item.entry)
            }
            if (!Auth.available(this@ManageActivity)) {
                Toast.makeText(this@ManageActivity, R.string.auth_unavailable, Toast.LENGTH_LONG).show()
                revert()
                return
            }
            // Mengaktifkan maupun mematikan kunci sama-sama meminta verifikasi.
            Auth.prompt(
                this@ManageActivity, getString(R.string.auth_title),
                onOk = { apply() }, onFail = { revert() },
            )
        }
    }

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_NOTIF = "notif"
        const val MODE_TASKS = "tasks"
        const val MODE_LOCK = "lock"
    }
}
