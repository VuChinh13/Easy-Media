package com.example.easymedia.ui.component.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easymedia.R
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.data.data_source.firebase.FirebaseAuthService
import com.example.easymedia.data.repository.AuthError
import com.example.easymedia.data.repository.AuthRepository
import com.example.easymedia.data.repository.AuthRepositoryImpl
import com.example.easymedia.utils.SharedPrefer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repo: AuthRepository = AuthRepositoryImpl(FirebaseAuthService(CloudinaryServiceImpl()))
) : ViewModel() {

    private val _loginResult = MutableSharedFlow<LoginEvent>()
    val loginResult = _loginResult.asSharedFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repo.login(email, password)
            result.onSuccess { uid ->
                // lấy thông tin và lưu vào SharedPreferences
                val result = repo.getUserById(uid)
                result.onSuccess { user ->
                    SharedPrefer.saveAllData(
                        uid,
                        user?.username,
                        user?.fullName,
                        user?.email,
                        user?.bio,
                        user?.location,
                        user?.gender,
                        user?.profilePicture
                    )
                }
                _loginResult.emit(LoginEvent.Success)
            }.onFailure { e ->
                when (e) {
                    is AuthError.Firebase -> _loginResult.emit(LoginEvent.Error(true))
                    else -> _loginResult.emit(LoginEvent.Error(false))
                }
            }
        }
    }
}

sealed class LoginEvent {
    object Success : LoginEvent()
    data class Error(val isErrorFirebase: Boolean) : LoginEvent()
}
