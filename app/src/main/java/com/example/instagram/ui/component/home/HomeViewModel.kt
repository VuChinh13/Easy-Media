package com.example.instagram.ui.component.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagram.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.instagram.data.data_source.firebase.FirebaseAuthService
import com.example.instagram.data.data_source.firebase.FirebasePostService
import com.example.instagram.data.model.Post
import com.example.instagram.data.model.User
import com.example.instagram.data.repository.AuthRepository
import com.example.instagram.data.repository.AuthRepositoryImpl
import com.example.instagram.data.repository.PostRepository
import com.example.instagram.data.repository.PostRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * lấy tất cả bài viết mà có trên firebase
 * Dữ liệu khi mà trả về lên là Pair (chứa data và message)
 */
class HomeViewModel : ViewModel() {

    private val repoPost: PostRepository =
        PostRepositoryImpl(FirebasePostService(cloudinary = CloudinaryServiceImpl()))
    private val repoAuth: AuthRepository = AuthRepositoryImpl(FirebaseAuthService())
    private val _posts = MutableLiveData<Pair<List<Post>, String>>()
    val posts: LiveData<Pair<List<Post>, String>> = _posts

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    // Lấy thông tin bài viết
    fun getAllPost() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repoPost.getAllPosts()
            result.onSuccess { list ->
                _posts.postValue(list to "")
            }.onFailure { e ->
                Log.d("Checknew", e.message.toString())
                _posts.postValue(emptyList<Post>() to (e.message ?: "Đã có lỗi xảy ra"))
            }
        }
    }

    // Lấy tất cả những người dùng của bài viết
    fun getUserOfPost(listPost: List<Post>) {
        viewModelScope.launch {
            listPost.forEach {
                val result = repoAuth.getUserById(it.userId)
                result.onSuccess {
                    _user.postValue(it)
                }
            }
        }
    }
}
