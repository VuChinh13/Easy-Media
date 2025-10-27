package com.example.easymedia.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Message(
    var id: String = "",

    @get:PropertyName("sender_id") @set:PropertyName("sender_id")
    var senderId: String = "",

    var content: String = "",

    var type: String = "text", // "text", "image", "video", "location"...

    @get:PropertyName("is_read") @set:PropertyName("is_read")
    var isRead: Boolean = false,

    @get:PropertyName("created_at") @set:PropertyName("created_at")
    @ServerTimestamp
    var createdAt: Date? = null
)
