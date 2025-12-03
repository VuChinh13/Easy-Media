package com.example.easymedia.data.model

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@IgnoreExtraProperties
@Parcelize
data class Story(
    // 🔹 ID document tự sinh từ Firestore
    @DocumentId
    var id: String = "",

    // 🔹 ID người đăng
    @get:PropertyName("user_id") @set:PropertyName("user_id")
    var userId: String = "",

    // 🔹 Link ảnh story (sau khi upload lên Cloudinary / Firebase Storage)
    @get:PropertyName("image_url") @set:PropertyName("image_url")
    var imageUrl: String = "",

    // 🔹 Đối tượng nhạc kèm theo (nếu có)
    var music: Music? = null,

    // 🔹 Thời gian tạo (Firebase sẽ tự gán server timestamp)
    @get:PropertyName("created_at") @set:PropertyName("created_at")
    @ServerTimestamp
    var createdAt: Date? = null,

    // 🔹 Thời gian hết hạn (mặc định +24h)
    @get:PropertyName("expire_at") @set:PropertyName("expire_at")
    var expireAt: Date? = null,

    @get:PropertyName("thumbnail_url") @set:PropertyName("thumbnail_url")
    var thumbnailUrl: String = "",

    @get:PropertyName("duration_ms") @set:PropertyName("duration_ms")
    var durationMs: Long = 0L
) : Parcelable
