package com.example.easymedia.ui.component.addpost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.data.data_source.firebase.FirebasePostService
import com.example.easymedia.data.model.Location
import com.example.easymedia.data.repository.PostRepository
import com.example.easymedia.data.repository.PostRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Tạo bài viết
 */
class AddPostViewModel : ViewModel() {
    private val repo: PostRepository = PostRepositoryImpl(FirebasePostService(cloudinary = CloudinaryServiceImpl()))
    private val _event = MutableSharedFlow<AddPostEvent>()
    val event = _event.asSharedFlow()
    fun createPost(
        userId: String,
        caption: String,
        location: Location?,
        imageFiles: List<File>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repo.createPostWithCloudinary(userId, caption, location, imageFiles)
            result.onSuccess {
                _event.emit(AddPostEvent.Success)
            }.onFailure { e ->
                _event.emit(AddPostEvent.Error)
            }
        }
    }
}


sealed class AddPostEvent {
    object Success : AddPostEvent()
    object Error : AddPostEvent()
}