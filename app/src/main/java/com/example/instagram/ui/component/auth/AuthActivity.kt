package com.example.instagram.ui.component.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.instagram.databinding.ActivityAuthBinding
import com.example.instagram.ui.component.main.MainActivity
import com.example.instagram.ui.component.signup.SignupActivity
import com.example.instagram.ui.component.utils.AppToast

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUi()
        observeViewModel()
    }

    private fun setupUi() {
        // Đăng ký
        binding.btSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        // Đăng nhập
        binding.btLogin.setOnClickListener {
            if (validateForm()) {
                Log.d("Checklog", "dang goi")
                authViewModel.login(
                    email = binding.etEmail.text?.toString()?.trim().orEmpty(),
                    password = binding.etPassword.text?.toString().orEmpty()
                )
            }
        }

        // Ấn Done trên bàn phím ở ô password -> submit
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.btLogin.performClick()
                true
            } else false
        }

        // Clear error khi gõ lại
        binding.etEmail.addTextChangedListener { binding.etEmail.error = null }
        // Với TextInputLayout, set error ở layout
        binding.etPassword.addTextChangedListener { binding.til.error = null }
    }

    private fun observeViewModel() {
        authViewModel.loginResult.observe(this) { result ->
            val (ok, message) = result
            if (ok) {
                // AppToast là tiện ích custom của bạn (như file trước)
                AppToast.show(this, message)
                 startActivity(Intent(this, MainActivity::class.java))
                 finish()
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Validate: email hợp lệ + password không rỗng */
    private fun validateForm(): Boolean {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val pass = binding.etPassword.text?.toString().orEmpty()

        var check = true

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Email không hợp lệ"
            check = false
        } else {
            binding.etEmail.error = null
        }

        if (pass.isBlank()) {
            binding.til.error = "Vui lòng nhập mật khẩu"
            check = false
        } else {
            binding.til.error = null
        }

        return check
    }
}
