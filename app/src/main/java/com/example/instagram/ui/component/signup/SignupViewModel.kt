package com.example.instagram.ui.component.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagram.data.data_source.firebase.FirebaseAuthService
import com.example.instagram.data.repository.AuthRepository
import com.example.instagram.data.repository.AuthRepositoryImpl
import com.example.instagram.data.repository.AuthError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignupViewModel(
    private val repo: AuthRepository = AuthRepositoryImpl(FirebaseAuthService())
) : ViewModel() {

    private val _signupResult = MutableLiveData<Pair<Boolean, String>>()
    val signupResult: LiveData<Pair<Boolean, String>> = _signupResult

    fun signup(
        email: String,
        password: String,
        username: String,
        fullName: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repo.signup(
                email = email,
                password = password,
                username = username,
                fullName = fullName
            )
            result.onSuccess {
                // it = uid
                _signupResult.postValue(true to "Đăng ký thành công")
            }.onFailure { e ->
                val message = when (e) {
                    is AuthError.UsernameTaken ->
                        "Tên người dùng đã tồn tại, vui lòng chọn tên khác"

                    is AuthError.Firebase ->
                        e.message ?: "Đăng ký thất bại, vui lòng thử lại"

                    else ->
                        e.message ?: "Đăng ký thất bại, vui lòng kiểm tra lại thông tin"
                }
                _signupResult.postValue(false to message)
            }
        }
    }
}
