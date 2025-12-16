package com.example.easymedia.ui.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.easymedia.data.model.User

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

    fun getId(): String {
        return getSharedPrefer().getString("_id", "") ?: ""
    }

    fun getUserName(): String {
        return getSharedPrefer().getString("username", "") ?: ""
    }

    fun getFullName(): String {
        return getSharedPrefer().getString("fullname", "") ?: ""
    }

    fun getProfilePicture(): String {
        return getSharedPrefer().getString("profilepicture", "") ?: ""
    }

    fun getEmail(): String {
        return getSharedPrefer().getString("email", "") ?: ""
    }

    fun getBio(): String {
        return getSharedPrefer().getString("bio", "") ?: ""
    }

    fun getLocation(): String {
        return getSharedPrefer().getString("location", "") ?: ""
    }

    fun getSharedPrefer(): SharedPreferences {
        return context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    }

    fun getUser(): User {
        return User(
            id = getId(),
            username = getUserName(),
            fullName = getFullName(),
            email = getEmail(),
            bio = getBio(),
            location = getLocation(),
            profilePicture = getProfilePicture(),
        )
    }

    // lưu dữ liệu
    fun saveAllData(
        id: String,
        username: String?,
        fullName: String?,
        email: String?,
        bio: String?,
        location: String?,
        gender: String?,
        profilePicture: String?
    ) {
        getSharedPrefer().edit {
            putString("_id", id)
            putString("username", username)
            putString("fullname", fullName)
            putString("email", email)
            putString("bio", bio)
            putString("location", location)
            putString("gender", gender)
            putString("profilepicture", profilePicture)
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

    fun saveFullName(fullName: String) {
        getSharedPrefer().edit {
            putString("fullname", fullName)
        }
    }

    fun saveProfilePicture(profilePicture: String) {
        getSharedPrefer().edit {
            putString("profilepicture", profilePicture)
        }
    }
}