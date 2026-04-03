package com.example.easymedia.extension

import java.text.Normalizer

object Strings {
    fun removeVietnameseAccents(str: String): String {
        var text = Normalizer.normalize(str, Normalizer.Form.NFD)
        text = text.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return text.replace("đ", "d").replace("Đ", "D")
    }
}