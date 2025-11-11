package com.example.easymedia.ui.permission

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionHelper(
    private val activity: Activity,
    private val onGranted: () -> Unit,
    private val onDenied: (() -> Unit)? = null
) {
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    // Gọi trong onCreate() của Activity hoặc Fragment để khởi tạo launcher
    fun registerLauncher(activity: Activity) {
        permissionLauncher =
            (activity as? androidx.activity.ComponentActivity)?.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val allGranted = permissions.values.all { it }
                if (allGranted) {
                    onGranted()
                } else {
                    onDenied?.invoke()
                }
            } ?: throw IllegalStateException("Activity must be ComponentActivity")
    }

    fun checkAndRequestPermissions() {
        val permissions = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO // nếu cần nhạc trong máy
            )
            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            onGranted()
        } else {
            permissionLauncher.launch(permissions)
        }
    }
}
