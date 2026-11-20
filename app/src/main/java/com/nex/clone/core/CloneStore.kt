package com.nex.clone.core

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/** Satu aplikasi yang sudah dikloning, beserta nama/ikon kustom dan status kunci/privasi. */
data class CloneEntry(
    val pkg: String,
    var name: String? = null,
    var iconFile: String? = null,
    var locked: Boolean = false,
    var priv: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
) {
    fun toJson(): JSONObject = JSONObject()
        .put("pkg", pkg).put("name", name ?: JSONObject.NULL).put("iconFile", iconFile ?: JSONObject.NULL)
        .put("locked", locked).put("priv", priv).put("addedAt", addedAt)

    companion object {
        fun fromJson(o: JSONObject) = CloneEntry(
            pkg = o.getString("pkg"),
            name = if (o.isNull("name")) null else o.optString("name"),
            iconFile = if (o.isNull("iconFile")) null else o.optString("iconFile"),
            locked = o.optBoolean("locked"),
            priv = o.optBoolean("priv"),
            addedAt = o.optLong("addedAt", System.currentTimeMillis()),
        )
    }
}

/** Penyimpanan daftar klon + riwayat penambahan (SharedPreferences, JSON). */
object CloneStore {
    private lateinit var sp: SharedPreferences

    fun init(ctx: Context) {
        if (!::sp.isInitialized) {
            sp = ctx.applicationContext.getSharedPreferences("nex_clone_store", Context.MODE_PRIVATE)
        }
    }

    @Synchronized
    fun all(): List<CloneEntry> {
        val raw = sp.getString("clones", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { CloneEntry.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun get(pkg: String): CloneEntry? = all().firstOrNull { it.pkg == pkg }

    @Synchronized
    private fun write(list: List<CloneEntry>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        sp.edit { putString("clones", arr.toString()) }
    }

    fun upsert(e: CloneEntry) {
        val list = all().toMutableList()
        val i = list.indexOfFirst { it.pkg == e.pkg }
        if (i >= 0) list[i] = e else list.add(e)
        write(list)
    }

    fun remove(pkg: String) {
        write(all().filter { it.pkg != pkg })
    }

    // ---- riwayat "Sejarah Tambahkan Rekor" ----
    fun history(): List<String> {
        val raw = sp.getString("history", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    fun addHistory(pkg: String) {
        val list = history().toMutableList()
        list.remove(pkg)
        list.add(0, pkg)
        val arr = JSONArray()
        list.take(12).forEach { arr.put(it) }
        sp.edit { putString("history", arr.toString()) }
    }

    fun clearHistory() {
        sp.edit { remove("history") }
    }
}
