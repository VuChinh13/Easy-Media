package com.example.easymedia.ui.component.viewstory

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.data.data_source.firebase.FirebaseAuthService
import com.example.easymedia.data.data_source.firebase.FirebasePostService
import com.example.easymedia.data.model.Post
import com.example.easymedia.data.model.User
import com.example.easymedia.data.repository.AuthRepositoryImpl
import com.example.easymedia.data.repository.PostRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ViewStoryViewModel : ViewModel() {
    private val repoAuth = AuthRepositoryImpl(FirebaseAuthService(CloudinaryServiceImpl()))
    private val _getInforUserResult = MutableLiveData<User?>()
    val getInforUserResult: LiveData<User?> = _getInforUserResult

    // Lấy thông tin người dùng
    fun getInforUser(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repoAuth.getUserById(uid)
            result.onSuccess { user ->
                _getInforUserResult.postValue(user)
            }.onFailure {
            }
        }
    }
}