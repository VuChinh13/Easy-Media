package com.example.easymedia.data.model

import android.net.Uri

data class VideoEditState(
    // ✅ 1. Âm thanh gốc
    val removeOriginalAudio: Boolean = false,   // true = xoá tiếng gốc

    // ✅ 2. Nhạc nền mới (nếu có)
    val newMusicUri: Uri? = null,               // null = không thêm nhạc mới
    val musicStartOffset: Long = 0L,            // thời điểm bắt đầu nhạc (ms)

    // ✅ 3. Giới hạn độ dài
    val maxDurationSec: Int = 40,               // độ dài tối đa
    val trimStartMs: Long = 0L,                 // nếu cần cắt đầu video
    val trimEndMs: Long? = null,                // cắt cuối video (<= 40s)

    // ✅ 4. Thêm text
    val overlayText: String? = null,            // null = không thêm text
    val textPositionX: Float = 0.5f,            // vị trí text (0–1 theo tỉ lệ)
    val textPositionY: Float = 0.8f,
    val textColor: String = "#FFFFFF",          // mã màu
    val textSizeSp: Float = 18f,
)
