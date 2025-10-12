package com.example.instagram.ui.component.addpost

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagram.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.instagram.data.data_source.firebase.FirebasePostService
import com.example.instagram.data.repository.PostRepository
import com.example.instagram.data.repository.PostRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Tạo bài viết
 */
class AddPostViewModel : ViewModel() {
    private val repo: PostRepository =
        PostRepositoryImpl(FirebasePostService(cloudinary = CloudinaryServiceImpl()))
    private val _result = MutableLiveData<Pair<Boolean, String>>()
    val result: LiveData<Pair<Boolean, String>> = _result

    fun createPost(
        userId: String,
        caption: String,
        location: String?,
        imageFiles: List<File>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repo.createPostWithCloudinary(userId, caption, location, imageFiles)
            result.onSuccess {
                // không cần quan tâm id bài viết trả về
                _result.postValue(true to "")
            }.onFailure { e ->
                 Log.d("CheckLoi",e.toString())
                _result.postValue(false to (e.message ?: "Đã có lỗi xảy ra"))
            }
        }
    }
}
