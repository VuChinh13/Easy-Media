package com.example.easymedia.ui.component.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.easymedia.R
import com.example.easymedia.ui.component.auth.AuthActivity
import com.example.easymedia.ui.component.main.MainActivity
import com.example.easymedia.utils.SharedPrefer

/**
 *  Luồng xin quyền
 *
 *  Kiểm tra quyền xem đã xin chưa
 *  Nếu chưa xin quyền thì xin lại
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Từ chối/Đồng ý -> Đều đi tiếp
        goToMain()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        requestNotificationPermissionNeeded()
    }

    private fun goToMain() {
        Handler(Looper.getMainLooper()).postDelayed({
            SharedPrefer.updateContext(this)
            if (SharedPrefer.getSharedPrefer().all.isEmpty()) {
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            } else {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }, 2000)
    }

    private fun requestNotificationPermissionNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            goToMain()
            return
        }

        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
                -> goToMain()

            else -> {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
