package com.example.easymedia.ui.component.postdetail

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.data.data_source.firebase.FirebasePostService
import com.example.easymedia.data.model.Post
import com.example.easymedia.data.repository.PostRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class PostDetailViewModel : ViewModel() {
    private val repoPost =
        PostRepositoryImpl(FirebasePostService(cloudinary = CloudinaryServiceImpl()))
    private val _getUserPostsResult = MutableLiveData<List<Post>>()
    val getUserPostsResult: LiveData<List<Post>> = _getUserPostsResult

    // Lấy tất cả bài viết của người dùng
    fun getUserPosts(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repoPost.getPostsByUser(userId)
            result.onSuccess { listPost ->
                Log.d("Checkb", "{it.message.toString()} thanh cong roi")
                _getUserPostsResult.postValue(listPost)
            }.onFailure {
                _getUserPostsResult.postValue(listOf())
            }
        }
    }
}
