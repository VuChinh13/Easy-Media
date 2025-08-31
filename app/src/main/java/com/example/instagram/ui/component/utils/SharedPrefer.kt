package com.example.instagram.ui.component.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * class dùng để lưu dữ liệu vào trong SharePreferences
 *
 * lấy dữ liệu từ SharePreferences
 * dùng context.applicationContext nhưng vẫn phải bắt đầu từ 1 context
 *
 */

@SuppressLint("StaticFieldLeak")
object SharedPrefer {
    lateinit var context: Context

    fun updateContext(context: Context) {
        this.context = context.applicationContext
    }

    fun getUserId(): String {
        return getSharedPrefer().getString("_id", "") ?: ""
    }

    fun getUserName(): String {
        return getSharedPrefer().getString("username", "") ?: ""
    }

    fun getName(): String {
        return getSharedPrefer().getString("name", "") ?: ""
    }

    fun getAvatar(): String {
        return getSharedPrefer().getString("avatar", "") ?: ""
    }

    fun getPassword(): String {
        return getSharedPrefer().getString("password", "") ?: ""
    }

    fun getGender(): String {
        return getSharedPrefer().getString("gender", "") ?: ""
    }

    fun getAddress(): String {
        return getSharedPrefer().getString("address", "") ?: ""
    }

    fun getIntroduce(): String {
        return getSharedPrefer().getString("introduce", "") ?: ""
    }

    fun getSharedPrefer(): SharedPreferences {
        return context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    }

    // lưu dữ liệu
    fun saveAllData(
        id: String,
        username: String,
        password: String,
        name: String,
        avatar: String,
        gender: String,
        address: String,
        introduce: String
    ) {
        getSharedPrefer().edit {
            putString("_id", id)
            putString("username", username)
            putString("password", password)
            putString("name", name)
            putString("avatar", avatar)
            putString("gender", gender)
            putString("address", address)
            putString("introduce", introduce)
        }
    }

    fun savePassword(password: String) {
        getSharedPrefer().edit {
            putString("password", password)
        }
    }

    fun saveUserName(userName: String) {
        getSharedPrefer().edit {
            putString("name", userName)
        }
    }

    fun saveAvatar(avatar: String) {
        getSharedPrefer().edit {
            putString("avatar", avatar)
        }
    }

    fun saveGender(gender: String) {
        getSharedPrefer().edit {
            putString("gender", gender)
        }
    }

    fun saveAddress(address: String) {
        getSharedPrefer().edit {
            putString("address", address)
        }
    }

    fun saveIntroduce(introduce: String) {
        getSharedPrefer().edit {
            putString("introduce", introduce)
        }
    }
}