//package com.example.instagram.ui.component.updatepost
//
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.instagram.data.model.PostResponse
//import com.example.instagram.data.repository.AuthRepository
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import java.io.File
//
//class UpdatePostViewModel : ViewModel() {
//    private val authRepository = AuthRepository()
//    private val _getPostUpdateResponse = MutableLiveData<PostResponse?>()
//    val getPostUpdateResponse: LiveData<PostResponse?> = _getPostUpdateResponse
//    fun updatePost(
//        userId: String,
//        postId: String,
//        images: List<File>?,
//        content: String?
//    ) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val result = authRepository.updatePost(userId, postId, images, content)
//            _getPostUpdateResponse.postValue(result)
//        }
//    }
//}
