package com.nex.clone

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nex.clone.core.CloneStore
import com.nex.clone.databinding.ActivityPickBinding
import com.nex.clone.databinding.ItemPickAppBinding
import com.nex.clone.databinding.ItemPickHistoryBinding
import com.nex.clone.databinding.ItemPickLetterBinding
import com.nex.clone.ui.applySystemBarsPadding

/** "Pilih aplikasi": riwayat, daftar A–Z dengan indeks, dan pencarian. */
class PickAppActivity : AppCompatActivity() {

    private data class AppItem(val pkg: String, val label: String, val icon: Drawable, val cloned: Boolean)

    private sealed class Row {
        data class Letter(val c: Char) : Row()
        object History : Row()
        data class App(val item: AppItem) : Row()
    }

    private lateinit var b: ActivityPickBinding
    private var apps: List<AppItem> = emptyList()
    private var rows: List<Row> = emptyList()
    private var query = ""
    private val adapter = Adapter()

    private val edit = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == RESULT_OK) {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPickBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarsPadding()

        b.list.layoutManager = LinearLayoutManager(this)
        b.list.adapter = adapter
        b.btnBack.setOnClickListener { finish() }
        b.btnSearch.setOnClickListener { setSearching(true) }
        b.btnSearchBack.setOnClickListener { setSearching(false) }
        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString().orEmpty()
                buildRows()
            }
        })

        b.indexBar.onLetter = { c -> scrollToLetter(c) }
        b.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val lm = rv.layoutManager as LinearLayoutManager
                b.indexBar.active = when (val r = rows.getOrNull(lm.findFirstVisibleItemPosition())) {
                    is Row.Letter -> r.c
                    is Row.App -> letterOf(r.item.label)
                    else -> null
                }
            }
        })

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (b.barSearch.visibility == View.VISIBLE) {
                    setSearching(false)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        load()
    }

    private fun load() {
        Thread {
            val pm = packageManager
            val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val cloned = CloneStore.all().map { it.pkg }.toSet()
            val list = pm.queryIntentActivities(launch, 0)
                .asSequence()
                .filter { it.activityInfo.packageName != packageName }
                .distinctBy { it.activityInfo.packageName }
                .map {
                    AppItem(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm),
                        it.activityInfo.packageName in cloned)
                }
                .sortedWith(compareBy({ letterOf(it.label) == '#' }, { it.label.lowercase() }))
                .toList()
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                apps = list
                b.progress.visibility = View.GONE
                b.list.visibility = View.VISIBLE
                buildRows()
            }
        }.start()
    }

    private fun letterOf(label: String): Char {
        val c = label.trim().firstOrNull()?.uppercaseChar() ?: return '#'
        return if (c in 'A'..'Z') c else '#'
    }

    private fun buildRows() {
        val out = ArrayList<Row>()
        if (query.isNotBlank()) {
            apps.filter { it.label.contains(query, ignoreCase = true) }.forEach { out.add(Row.App(it)) }
        } else {
            val hist = CloneStore.history().mapNotNull { p -> apps.firstOrNull { it.pkg == p && !it.cloned } }
            if (hist.isNotEmpty()) {
                out.add(Row.History)
                hist.forEach { out.add(Row.App(it)) }
            }
            var last: Char? = null
            for (a in apps) {
                val c = letterOf(a.label)
                if (c != last) {
                    out.add(Row.Letter(c))
                    last = c
                }
                out.add(Row.App(a))
            }
        }
        rows = out
        adapter.notifyDataSetChanged()
        b.indexBar.visibility = if (query.isBlank()) View.VISIBLE else View.GONE
    }

    private fun scrollToLetter(c: Char) {
        fun key(x: Char) = if (x == '#') '\uFFFF' else x
        val pos = rows.indexOfFirst { it is Row.Letter && key(it.c) >= key(c) }
        if (pos >= 0) (b.list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(pos, 0)
    }

    private fun setSearching(on: Boolean) {
        b.barNormal.visibility = if (on) View.GONE else View.VISIBLE
        b.barSearch.visibility = if (on) View.VISIBLE else View.GONE
        val imm = getSystemService(InputMethodManager::class.java)
        if (on) {
            b.etSearch.requestFocus()
            imm.showSoftInput(b.etSearch, InputMethodManager.SHOW_IMPLICIT)
        } else {
            b.etSearch.setText("")
            imm.hideSoftInputFromWindow(b.etSearch.windowToken, 0)
        }
    }

    private fun clone(item: AppItem) {
        edit.launch(Intent(this, EditCloneActivity::class.java).putExtra(EditCloneActivity.EXTRA_PKG, item.pkg))
    }

    private inner class Adapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount() = rows.size

        override fun getItemViewType(position: Int) = when (rows[position]) {
            is Row.Letter -> 0
            is Row.History -> 1
            is Row.App -> 2
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inf = LayoutInflater.from(parent.context)
            return when (viewType) {
                0 -> LetterVH(ItemPickLetterBinding.inflate(inf, parent, false))
                1 -> HistoryVH(ItemPickHistoryBinding.inflate(inf, parent, false))
                else -> AppVH(ItemPickAppBinding.inflate(inf, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val r = rows[position]) {
                is Row.Letter -> (holder as LetterVH).vb.tvLetter.text = r.c.toString()
                is Row.History -> (holder as HistoryVH).vb.tvClear.setOnClickListener {
                    CloneStore.clearHistory()
                    buildRows()
                }
                is Row.App -> {
                    val vb = (holder as AppVH).vb
                    vb.imgIcon.setImageDrawable(r.item.icon)
                    vb.tvLabel.text = r.item.label
                    vb.btnAdd.visibility = if (r.item.cloned) View.GONE else View.VISIBLE
                    vb.tvDone.visibility = if (r.item.cloned) View.VISIBLE else View.GONE
                    vb.root.setOnClickListener { if (!r.item.cloned) clone(r.item) }
                }
            }
        }
    }

    private class LetterVH(val vb: ItemPickLetterBinding) : RecyclerView.ViewHolder(vb.root)
    private class HistoryVH(val vb: ItemPickHistoryBinding) : RecyclerView.ViewHolder(vb.root)
    private class AppVH(val vb: ItemPickAppBinding) : RecyclerView.ViewHolder(vb.root)
}
