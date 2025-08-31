package com.example.instagram.ui.component.utils

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import com.example.instagram.databinding.CustomToastBinding

object AppToast {
    fun show(context:Context, message: String){
        val binding = CustomToastBinding.inflate(LayoutInflater.from(context))
        binding.tvMessage.text = message

        Toast(context).apply {
            duration  = Toast.LENGTH_SHORT
            view = binding.root
            show()
        }
    }
}