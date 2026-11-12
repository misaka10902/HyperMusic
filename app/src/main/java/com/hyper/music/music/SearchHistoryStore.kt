package com.hyper.music.music

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray


object SearchHistoryStore {
    private const val PREFS = "search_history_prefs"
    private const val KEY = "queries"
    private const val MAX_ITEMS = 10

    private val _items = MutableStateFlow<List<String>>(emptyList())
    val items: StateFlow<List<String>> = _items

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _items.value = runCatching {
            val arr = JSONArray(prefs.getString(KEY, null) ?: "[]")
            (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    fun add(raw: String) {
        val q = raw.trim()
        if (q.isEmpty()) return
        val list = _items.value.toMutableList()

        if (list.isNotEmpty() && q.length > list[0].length &&
            q.startsWith(list[0], ignoreCase = true)
        ) {
            list.removeAt(0)
        }
        list.remove(q)
        list.add(0, q)
        while (list.size > MAX_ITEMS) list.removeAt(list.lastIndex)
        _items.value = list
        persist()
    }


    fun remove(raw: String) {
        val q = raw.trim()
        if (q.isEmpty()) return
        val list = _items.value.toMutableList()
        if (list.remove(q)) {
            _items.value = list
            persist()
        }
    }

    fun clear() {
        _items.value = emptyList()
        persist()
    }

    private fun persist() {
        if (!::prefs.isInitialized) return
        prefs.edit().putString(KEY, JSONArray(_items.value).toString()).apply()
    }
}
