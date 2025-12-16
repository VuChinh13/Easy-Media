package com.example.easymedia.ui.component.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.easymedia.R
import com.example.easymedia.ui.component.auth.AuthActivity
import com.example.easymedia.ui.component.main.MainActivity
import com.example.easymedia.ui.utils.SharedPrefer

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Handler(Looper.getMainLooper()).postDelayed({
            SharedPrefer.updateContext(this)
            if (SharedPrefer.getSharedPrefer().all.isEmpty()) {
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            }
            else {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }, 2000)
    }
}
