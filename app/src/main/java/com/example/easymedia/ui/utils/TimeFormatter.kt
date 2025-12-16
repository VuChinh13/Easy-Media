package com.example.easymedia.ui.utils

import android.text.format.DateUtils
import java.util.*
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
/**
 * Chuyển đổi đối tượng Date? thành chuỗi "thời gian tương đối"
 * Ví dụ: "Vừa xong", "5 phút trước", "2 giờ trước", "Hôm qua", ...
 */
object TimeFormatter {

    fun getRelativeTime(date: Date?): String {
        if (date == null) return "Đang cập nhật"

        val now = System.currentTimeMillis()
        val time = date.time

        // DateUtils tự động trả về chuỗi theo khoảng cách thời gian
        val relativeTime = DateUtils.getRelativeTimeSpanString(
            time,
            now,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        )

        return relativeTime.toString()
    }

    fun formatTimeAgo(date: Date): String {
        val diffMillis = System.currentTimeMillis() - date.time
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diffMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
        val days = TimeUnit.MILLISECONDS.toDays(diffMillis)

        return when {
            seconds < 60 -> "Vừa xong"
            minutes < 60 -> "$minutes phút"
            hours < 24 -> "$hours tiếng"
            days == 1L -> "Hôm qua"
            days < 7 -> "$days ngày trước"
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
        }
    }

}
