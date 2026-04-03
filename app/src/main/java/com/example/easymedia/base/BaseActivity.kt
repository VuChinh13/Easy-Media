package com.example.easymedia.base

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.viewbinding.ViewBinding
import com.example.easymedia.R

abstract class BaseActivity<VB : ViewBinding, VM : ViewModel> : AppCompatActivity() {
    private var _binding: VB? = null
    protected val binding get() = _binding!!
    protected abstract val viewModel: VM
    abstract fun inflateBinding(): VB
    private var loadingDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logLifecycleActivity()
        _binding = inflateBinding()
        setContentView(binding.root)
        initView(savedInstanceState)
        initData()
    }

    open fun initView(savedInstanceState: Bundle?) {}
    open fun initData() {}

    fun showLoading() {
        if (isFinishing || isDestroyed) return
        if (loadingDialog?.isShowing == true) return

        loadingDialog = Dialog(this, R.style.DialogFullscreen).apply {
            WindowCompat.getInsetsController(window!!, window!!.decorView)
                .hide(WindowInsetsCompat.Type.systemBars())
            setContentView(R.layout.dialog_loading)
            setCancelable(false)
            show()
        }
    }

    fun hideLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    override fun onDestroy() {
        super.onDestroy()
        logLifecycleActivity()
    }

    private fun logLifecycleActivity() {
        val methodName = Throwable().stackTrace[1].methodName
        val className = this::class.simpleName.toString()
        Log.d(TAG, "$methodName - $className")
    }

    companion object {
        const val TAG = "WriteLogActivity"
    }
}