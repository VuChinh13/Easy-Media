package com.example.instagram.ui.component.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.instagram.databinding.ActivityAuthBinding
import com.example.instagram.ui.component.main.MainActivity
import com.example.instagram.ui.component.signup.SignupActivity
import com.example.instagram.ui.component.utils.SharedPrefer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private val authViewModel: AuthViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
        binding.btLogin.setOnClickListener {
            authViewModel.login(
                binding.etUserName.text.toString().trim(),
                binding.etPassword.text.toString().trim()
            )
        }

        
        authViewModel.errorMessage.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }
        authViewModel.loginResult.observe(this) { result ->
            if (result != null) {
                Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                // lưu thông tin đăng nhập vào trong SharedPreferences
                SharedPrefer.updateContext(this)
                SharedPrefer.saveAllData(
                    result.data._id.toString(),
                    result.data.username.toString(),
                    result.data.password.toString(),
                    result.data.name.toString(),
                    result.data.avatar.toString(),
                    result.data.gender.toString(),
                    result.data.address.toString(),
                    result.data.introduce.toString()
                )
                lifecycleScope.launch {
                    delay(1000)
                    startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                    finish() // Đóng SplashActivity
                }
            } else {
                Toast.makeText(this, "Đã có lỗi xảy ra", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
