//package com.example.instagram.ui.component.profile
//
//import android.util.Log
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.instagram.data.model.PostResponse
//import com.example.instagram.data.repository.AuthRepository
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//
//class ProfileViewModel : ViewModel() {
//    private val authRepository = AuthRepository()
//    private val _getInforUserResult = MutableLiveData<InforUserResponse?>()
//    val getInforUserResult: LiveData<InforUserResponse?> = _getInforUserResult
//
//    private val _getUserPostsResult = MutableLiveData<PostResponse?>()
//    val getUserPostsResult: LiveData<PostResponse?> = _getUserPostsResult
//
//
//    fun getInforUser(username: String) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val result = authRepository.getInforUser(username)
//            _getInforUserResult.postValue(result)
//        }
//    }
//
//    fun getUserPosts(username: String) {
//        viewModelScope.launch(Dispatchers.IO){
//            val result = authRepository.getUserPosts(username)
//            _getUserPostsResult.postValue(result)
//        }
//    }
//}
