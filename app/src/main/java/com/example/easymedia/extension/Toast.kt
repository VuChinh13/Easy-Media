package com.example.easymedia.extension

import android.app.Activity
import android.widget.Toast
import androidx.fragment.app.Fragment

fun Fragment.showToast(resId: Int, isShort: Boolean = true) {
    Toast.makeText(
        requireContext(),
        getString(resId),
        if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
    ).show()
}

fun Fragment.showToast(message: String, isShort: Boolean = true) {
    Toast.makeText(
        requireContext(),
        message,
        if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
    ).show()
}

fun Activity.showToast(resId: Int, isShort: Boolean = true) {
    Toast.makeText(
        this,
        getString(resId),
        if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
    ).show()
}

fun Activity.showToast(message: String, isShort: Boolean = true) {
    Toast.makeText(
        this,
        message,
        if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
    ).show()
}