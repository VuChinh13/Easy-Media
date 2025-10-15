package com.example.instagram.ui.component.auth

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagram.data.data_source.firebase.FirebaseAuthService
import com.example.instagram.data.repository.AuthRepository
import com.example.instagram.data.repository.AuthRepositoryImpl
import com.example.instagram.data.repository.AuthError
import com.example.instagram.ui.component.utils.SharedPrefer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repo: AuthRepository = AuthRepositoryImpl(FirebaseAuthService())
) : ViewModel() {

    private val _loginResult = MutableLiveData<Pair<Boolean, String>>()
    val loginResult: LiveData<Pair<Boolean, String>> = _loginResult

    fun login(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repo.login(email, password)
            result.onSuccess { uid ->
                // it = uid (nếu service trả về uid)
                _loginResult.postValue(true to "Đăng nhập thành công")

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
                        user?.profilePicture
                    )
                }.onFailure {

                }
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
