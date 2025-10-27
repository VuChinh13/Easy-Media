package com.example.easymedia.ui.component.signup

import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.easymedia.databinding.ActivitySignupBinding
import com.example.easymedia.ui.component.utils.AppToast

@Suppress("DEPRECATION")
class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val signupViewModel: SignupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUi()
        observeViewModel()
    }

    private fun setupUi() {
        // Signup button
        binding.btSignup.setOnClickListener {
            if (validateForm()) {
                signupViewModel.signup(
                    email = binding.etEmail.text?.toString()?.trim().orEmpty(),
                    password = binding.etPassword.text?.toString().orEmpty(),
                    username = binding.etUsername.text?.toString()?.trim().orEmpty(),
                    fullName = binding.etName.text?.toString()?.trim().orEmpty()
                )
            }
        }

        // Editor action on confirm password -> click signup
        binding.etConfirmPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.btSignup.performClick()
                true
            } else false
        }

        // Clear errors as user types
        binding.etEmail.addTextChangedListener { binding.etEmail.error = null }
        binding.etUsername.addTextChangedListener { binding.etUsername.error = null }
        binding.etPassword.addTextChangedListener { binding.til.error = null }
        binding.etConfirmPassword.addTextChangedListener { binding.til1.error = null }
        binding.etName.addTextChangedListener { /* no error field here, nên bỏ trống */ }
    }

    private fun observeViewModel() {
        signupViewModel.signupResult.observe(this) { result ->
            val (ok, message) = result
            if (ok) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                Log.d("CheckFB",message)
                finish() // quay lại màn hình trước (vd: AuthActivity)
            } else {
                Log.d("CheckFB",message)
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateForm(): Boolean {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val username = binding.etUsername.text?.toString()?.trim().orEmpty()
        val fullName = binding.etName.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirm = binding.etConfirmPassword.text?.toString().orEmpty()

        var check = true

        // Email
        if (!isValidEmail(email)) {
            binding.etEmail.error = "Email không hợp lệ"
            check = false
        } else {
            binding.etEmail.error = null
        }

        // Username
        if (!isValidUsername(username)) {
            binding.etUsername.error = "Username 3–20 ký tự (a–z, 0–9, . hoặc _)"
            check = false
        } else {
            binding.etUsername.error = null
        }

        // Full name
        if (fullName.isBlank()) {
            Toast.makeText(this, "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show()
            check = false
        }

        // Password
        if (!isStrongPassword(password)) {
            binding.til.error = "Mật khẩu ≥ 8 ký tự, gồm chữ & số"
            check = false
        } else {
            binding.til.error = null
        }

        // Confirm
        if (confirm != password) {
            binding.til1.error = "Mật khẩu nhập lại không khớp"
            check = false
        } else {
            binding.til1.error = null
        }

        return check
    }

    // Helpers
    private fun isValidEmail(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun isValidUsername(username: String): Boolean =
        Regex("^[a-zA-Z0-9._]{3,20}$").matches(username)

    private fun isStrongPassword(pw: String): Boolean =
        pw.length >= 8 && pw.any { it.isDigit() } && pw.any { it.isLetter() }
}
