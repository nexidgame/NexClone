package com.nex.clone

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.nex.clone.core.ApkPaths
import com.nex.clone.core.CloneEntry
import com.nex.clone.core.CloneStore
import com.nex.clone.core.IconUtils
import com.nex.clone.core.ProfileOpClient
import com.nex.clone.core.ProfileOps
import com.nex.clone.core.message
import com.nex.clone.databinding.ActivityEditBinding
import com.nex.clone.ui.Busy
import com.nex.clone.ui.OpFeedback
import com.nex.clone.ui.applySystemBarsPadding

/** "Ubah nama/ikon": dipakai sebelum mengkloning (tombol Klon) dan untuk mengubah klon yang ada. */
class EditCloneActivity : AppCompatActivity() {

    private lateinit var b: ActivityEditBinding
    private val ops = ProfileOpClient(this)

    private lateinit var pkg: String
    private var edit = false
    private var entry: CloneEntry? = null
    private var defaultName = ""
    private var pickedFile: String? = null
    private var resetIcon = false

    private val picker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val name = IconUtils.saveFromUri(this, pkg, uri)
            if (name != null) {
                pickedFile = name
                resetIcon = false
                showIcon()
            } else {
                Toast.makeText(this, R.string.icon_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pkg = intent.getStringExtra(EXTRA_PKG) ?: run {
            finish()
            return
        }
        edit = intent.getBooleanExtra(EXTRA_EDIT, false)
        entry = CloneStore.get(pkg) ?: if (edit) CloneEntry(pkg = pkg) else null

        b = ActivityEditBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarsPadding()
        b.toolbar.setNavigationOnClickListener { finish() }

        val pm = packageManager
        defaultName = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }
            .getOrDefault(pkg)
        b.etName.setText(entry?.name ?: defaultName)
        b.btnClone.setText(if (edit) R.string.edit_save else R.string.edit_clone)
        showIcon()

        b.btnClearName.setOnClickListener { b.etName.setText("") }
        b.tvPickIcon.setOnClickListener {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        b.tvResetIcon.setOnClickListener {
            pickedFile = null
            resetIcon = true
            showIcon()
        }
        b.btnClone.setOnClickListener { submit() }
    }

    private fun showIcon() {
        val file = pickedFile ?: if (resetIcon) null else entry?.iconFile
        val custom = IconUtils.loadCustom(this, file)
        if (custom != null) {
            b.imgIcon.setImageBitmap(custom)
        } else {
            b.imgIcon.setImageDrawable(runCatching { packageManager.getApplicationIcon(pkg) }.getOrNull())
        }
        b.tvResetIcon.visibility = if (custom != null) View.VISIBLE else View.GONE
    }

    private fun submit() {
        val typed = b.etName.text.toString().trim().ifEmpty { defaultName }
        val customName = if (typed != defaultName) typed else null

        if (edit) {
            val e = entry ?: return
            e.name = customName
            if (resetIcon) {
                IconUtils.delete(this, e.iconFile)
                e.iconFile = null
            }
            if (pickedFile != null) e.iconFile = pickedFile
            CloneStore.upsert(e)
            Toast.makeText(this, R.string.edit_saved, Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
            return
        }

        val busy = Busy.show(this, R.string.cloning)
        ops.run(ProfileOps.OP_CLONE, listOf(pkg), ApkPaths.of(this, pkg)) { r ->
            busy.dismiss()
            if (r.isOk) {
                CloneStore.upsert(CloneEntry(pkg = pkg, name = customName, iconFile = pickedFile))
                CloneStore.addHistory(pkg)
                Toast.makeText(this, R.string.clone_done, Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                OpFeedback.failed(this, r, ops.lastReason, R.string.clone_failed)
            }
        }
    }

    companion object {
        const val EXTRA_PKG = "pkg"
        const val EXTRA_EDIT = "edit"
    }
}
