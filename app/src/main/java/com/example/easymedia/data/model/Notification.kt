package com.example.easymedia.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.sql.Date

@IgnoreExtraProperties
data class Notification(
    @DocumentId
    val id: String = "",

    @get:PropertyName("receiver_id") @set:PropertyName("receiver_id")
    var receiverId: String = "",

    @get:PropertyName("sender_id") @set:PropertyName("sender_id")
    var senderId: String = "",

    val type: String = "",

    val postId: String? = null,
    val commentId: String? = null,
    val message: String = "",

    @get:PropertyName("is_read") @set:PropertyName("is_read")
    var isRead: Boolean = false,

    @get:PropertyName("created_at") @set:PropertyName("created_at")
    @ServerTimestamp
    var createdAt: Date? = null
)

