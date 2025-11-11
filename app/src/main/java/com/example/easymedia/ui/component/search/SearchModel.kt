package com.example.easymedia.ui.component.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.data.data_source.firebase.FirebaseAuthService
import com.example.easymedia.data.model.User
import com.example.easymedia.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val repoAuth = AuthRepositoryImpl(FirebaseAuthService(CloudinaryServiceImpl()))

    // Expose List<User> (không để MutableList ở public)
    private val _getInforUserResults = MutableLiveData<List<User>>(emptyList())
    val getInforUserResults: LiveData<List<User>> = _getInforUserResults

    private val _getUserById = MutableLiveData<User>()
    val getUserById: MutableLiveData<User> = _getUserById

    fun getListUser() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repoAuth.getAllUsers()
            if (result.isNotEmpty()) {
                _getInforUserResults.postValue(result)
            }
        }
    }

    fun getUserById(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repoAuth.getUserById(user.id)
            result.onSuccess { user ->
                if (user != null) {
                    _getUserById.postValue(user)
                }
            }
        }
    }
}
