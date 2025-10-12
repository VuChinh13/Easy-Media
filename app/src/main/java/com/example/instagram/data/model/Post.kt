package com.example.instagram.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class Counts(
    var comments: Int = 0,
    var likes: Int = 0
)

@IgnoreExtraProperties
data class Post(

    // Lấy document id từ Firestore
    @DocumentId
    var id: String = "",

    // Map userId <-> user_id (CẦN cả get: và set:)
    @get:PropertyName("user_id") @set:PropertyName("user_id")
    var userId: String = "",

    var caption: String = "",
    var location: String? = null,

    // Map imagePublicIds <-> image_public_ids
    @get:PropertyName("image_public_ids") @set:PropertyName("image_public_ids")
    var imagePublicIds: List<String> = emptyList(),

    // Map imageUrls <-> image_urls
    @get:PropertyName("image_urls") @set:PropertyName("image_urls")
    var imageUrls: List<String> = emptyList(),

    var counts: Counts = Counts(),

    @get:PropertyName("created_at") @set:PropertyName("created_at")
    @ServerTimestamp
    var createdAt: Date? = null,

    @get:PropertyName("updated_at") @set:PropertyName("updated_at")
    @ServerTimestamp
    var updatedAt: Date? = null

)
