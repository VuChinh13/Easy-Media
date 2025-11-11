package com.example.easymedia.ui.component.search

import android.content.Context
import com.example.easymedia.data.model.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SearchHistoryManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveUserToHistory(user: User) {
        val current = getSearchHistory().toMutableList()
        current.removeAll { it.id == user.id } // tránh trùng
        current.add(0, user) // thêm vào đầu danh sách
        if (current.size > 10) current.removeAt(current.lastIndex) // chỉ giữ tối đa 10
        val json = gson.toJson(current)
        prefs.edit().putString("history_list", json).apply()
    }

    fun getSearchHistory(): List<User> {
        val json = prefs.getString("history_list", null) ?: return emptyList()
        val type = object : TypeToken<List<User>>() {}.type
        return gson.fromJson(json, type)
    }

    fun clearHistory() {
        prefs.edit().remove("history_list").apply()
    }

    fun removeUserFromHistory(userId: String) {
        val current = getSearchHistory().toMutableList()
        val updated = current.filterNot { it.id == userId } // lọc bỏ user có id trùng
        val json = gson.toJson(updated)
        prefs.edit().putString("history_list", json).apply()
    }
}