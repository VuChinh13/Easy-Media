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
data class User(
    @DocumentId
    val id: String = "",

    val username: String = "",

    @get:PropertyName("full_name") @set:PropertyName("full_name")
    var fullName: String = "",

    val email: String = "",
    var gender: String? = null,
    val bio: String = "",
    val location: String? = null,

    @get:PropertyName("profile_picture") @set:PropertyName("profile_picture")
    var profilePicture: String? = null,

    // Dùng Long để hợp với FieldValue.increment()
    @get:PropertyName("post_count") @set:PropertyName("post_count")
    var postCount: Long = 0L,

    var followers: List<String> = emptyList(),
    var following: List<String> = emptyList(),

    @get:PropertyName("created_at") @get:ServerTimestamp
    var createdAt: Date? = null,

    @get:PropertyName("updated_at") @get:ServerTimestamp
    var updatedAt: Date? = null
) : Parcelable