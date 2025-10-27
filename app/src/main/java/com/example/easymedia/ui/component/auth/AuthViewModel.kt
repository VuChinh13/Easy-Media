package com.example.easymedia.ui.component.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.data.data_source.firebase.FirebaseAuthService
import com.example.easymedia.data.repository.AuthRepository
import com.example.easymedia.data.repository.AuthRepositoryImpl
import com.example.easymedia.data.repository.AuthError
import com.example.easymedia.ui.component.utils.SharedPrefer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel(
    private val repo: AuthRepository = AuthRepositoryImpl(FirebaseAuthService(CloudinaryServiceImpl()))
) : ViewModel() {

    private val _loginResult = MutableLiveData<Pair<Boolean, String>>()
    val loginResult: LiveData<Pair<Boolean, String>> = _loginResult

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
                // it = uid (nếu service trả về uid)
                _loginResult.postValue(true to "Đăng nhập thành công")
            }.onFailure { e ->
                val message = when (e) {
                    is AuthError.Firebase -> e.message ?: "Đăng nhập thất bại, vui lòng thử lại"
                    else -> e.message ?: "Đăng nhập thất bại, vui lòng kiểm tra lại thông tin"
                }
                _loginResult.postValue(false to message)
            }
        }
    }
}
