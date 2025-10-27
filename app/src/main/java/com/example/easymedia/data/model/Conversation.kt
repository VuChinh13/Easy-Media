package com.example.easymedia.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Conversation(
    var id: String = "",

    var participants: List<String> = emptyList(),

    @get:PropertyName("last_message") @set:PropertyName("last_message")
    var lastMessage: String = "",

    @get:PropertyName("updated_at") @set:PropertyName("updated_at")
    @ServerTimestamp
    var updatedAt: Date? = null
)