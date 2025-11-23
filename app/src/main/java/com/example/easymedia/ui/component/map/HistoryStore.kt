package com.example.easymedia.ui.component.map

object HistoryStore {
    private const val PREFS_NAME = "map_search_history"
    private const val KEY_HISTORY = "key_history"
    private const val MAX_HISTORY = 30

    private val gson = com.google.gson.Gson()

    fun loadHistory(context: android.content.Context): MutableList<HistoryItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY, null) ?: return mutableListOf()
        return try {
            val type = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, HistoryItem::class.java).type
            gson.fromJson<List<HistoryItem>>(json, type).toMutableList()
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    fun saveHistory(context: android.content.Context, list: List<HistoryItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }

    fun addOrMoveToTop(context: android.content.Context, newItem: HistoryItem) {
        val list = loadHistory(context)
        // Remove duplicate by address + coords (you can change equality rule)
        val existingIndex = list.indexOfFirst { it.address == newItem.address && it.lat == newItem.lat && it.lng == newItem.lng }
        if (existingIndex != -1) list.removeAt(existingIndex)
        list.add(0, newItem) // add to top
        // keep size limit
        if (list.size > MAX_HISTORY) list.subList(MAX_HISTORY, list.size).clear()
        saveHistory(context, list)
    }
}
