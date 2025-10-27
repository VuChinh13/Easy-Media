package com.example.easymedia.ui.component.utils

import android.text.format.DateUtils
import java.util.*

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
}
