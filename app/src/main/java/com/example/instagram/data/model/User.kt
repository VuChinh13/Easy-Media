package com.example.instagram.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class User(
    @DocumentId
    val id: String = "",

    val username: String = "",

    @get:PropertyName("full_name") @set:PropertyName("full_name")
    var fullName: String = "",

    val email: String = "",
    val bio: String = "",
    val location: String? = null,

    @get:PropertyName("profile_picture") @set:PropertyName("profile_picture")
    var profilePicture: String? = null,

    // Dùng Long để hợp với FieldValue.increment()
    @get:PropertyName("post_count") @set:PropertyName("post_count")
    var postCount: Long = 0L,

    val followers: List<String> = emptyList(),
    val following: List<String> = emptyList(),

    @get:PropertyName("created_at") @get:ServerTimestamp
    var createdAt: Date? = null,

    @get:PropertyName("updated_at") @get:ServerTimestamp
    var updatedAt: Date? = null
)
