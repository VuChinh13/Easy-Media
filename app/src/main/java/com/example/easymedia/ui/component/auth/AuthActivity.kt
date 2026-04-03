package com.example.easymedia.ui.component.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.core.widget.addTextChangedListener
import com.example.easymedia.R
import com.example.easymedia.base.BaseActivity
import com.example.easymedia.databinding.ActivityAuthBinding
import com.example.easymedia.extension.observe
import com.example.easymedia.extension.showToast
import com.example.easymedia.ui.component.main.MainActivity
import com.example.easymedia.ui.component.signup.SignupActivity

class AuthActivity : BaseActivity<ActivityAuthBinding, AuthViewModel>() {
    override val viewModel: AuthViewModel by viewModels()

    override fun inflateBinding(): ActivityAuthBinding {
        return ActivityAuthBinding.inflate(layoutInflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        setupUi()
    }

    override fun initData() {
        super.initData()
        observeViewModel()
    }

    private fun setupUi() {
        with(binding) {
            btSignup.setOnClickListener {
                startActivity(Intent(this@AuthActivity, SignupActivity::class.java))
            }

            btLogin.setOnClickListener {
                if (validateForm()) {
                    viewModel.login(
                        email = etEmail.text?.toString()?.trim().orEmpty(),
                        password = etPassword.text?.toString().orEmpty()
                    )
                }
            }

            etPassword.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    btLogin.performClick()
                    true
                } else false
            }

            etEmail.addTextChangedListener { etEmail.error = null }
            etPassword.addTextChangedListener { binding.til.error = null }
        }
    }

    private fun observeViewModel() {
        viewModel.loginResult.observe(this) { result ->
            when (result) {
                is LoginEvent.Error -> {
                    if (result.isErrorFirebase) {
                        showToast(R.string.error_login_failed)
                    } else showToast(R.string.error_login_invalid_credentials)
                }

                is LoginEvent.Success -> {
                    showToast(R.string.login_success)
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }

            }
        }
    }

    /** Validate: email hợp lệ + password không rỗng */
    private fun validateForm(): Boolean {
        with(binding) {
            val email = etEmail.text?.toString()?.trim().orEmpty()
            val pass = etPassword.text?.toString().orEmpty()

            var check = true

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = getString(R.string.email_error)
                check = false
            } else {
                etEmail.error = null
            }

            if (pass.isBlank()) {
                til.error = getString(R.string.error_enter_password)
                check = false
            } else {
                til.error = null
            }

            return check
        }
    }
}
