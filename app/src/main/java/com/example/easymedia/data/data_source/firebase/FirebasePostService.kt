package com.example.easymedia.data.data_source.firebase

import android.util.Log
import com.example.easymedia.data.data_source.cloudinary.CloudinaryService
import com.example.easymedia.data.model.Comment
import com.example.easymedia.data.model.Like
import com.example.easymedia.data.model.Location
import com.example.easymedia.data.model.Post
import com.example.easymedia.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

interface PostService {
    suspend fun createPost(
        userId: String,
        caption: String,
        location: Location?,
        imageUrls: List<String>,
        imagePublicIds: List<String> = emptyList()
    ): String

    /**
     * Lấy posts theo trang (descending by created_at).
     * @param pageSize số item mỗi trang
     * @param startAfterDoc nếu null => lấy trang đầu, nếu không => startAfter(startAfterDoc)
     * @return Pair(posts, lastVisibleDocumentSnapshotOrNull)
     */
    suspend fun getPostsPaginated(
        pageSize: Int,
        startAfterDoc: DocumentSnapshot? = null
    ): Pair<List<Post>, DocumentSnapshot?>

    suspend fun getPost(postId: String): Post?
    suspend fun getPostsByUser(userId: String): List<Post>
    suspend fun getAllPosts(): List<Post>
    suspend fun addComment(postId: String, userId: String, content: String): String
    suspend fun likePost(postId: String, userId: String)
    suspend fun deletePost(postId: String, userId: String)
    suspend fun unlikePost(postId: String, userId: String)
    suspend fun getUsersWhoLiked(postId: String): List<User>
    suspend fun hasUserLiked(postId: String, userId: String): Boolean
    suspend fun deleteComment(postId: String, commentId: String)
    suspend fun updatePost(
        existingPost: Post,
        removeImageUrls: List<String>,
        newCaption: String?
    )

    suspend fun getComments(postId: String): List<Comment>

    // Tạo post từ file ảnh (upload Cloudinary → lưu Firestore)
    suspend fun createPostWithCloudinary(
        userId: String,
        caption: String,
        location: Location?,
        imageFiles: List<File>
    ): String
}

class FirebasePostService(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val cloudinary: CloudinaryService // inject Cloudinary
) : PostService {

    // imagePublicIds cần để có thể xóa ảnh trên Cloudinary
    override suspend fun createPost(
        userId: String,
        caption: String,
        location: Location?,
        imageUrls: List<String>,
        imagePublicIds: List<String>
    ): String {
        val docRef = db.collection("posts").document()

        // 1️⃣ Tạo object post như cũ
        val post = Post(
            id = docRef.id,
            userId = userId,
            caption = caption,
            location = location,
            imageUrls = imageUrls,
            imagePublicIds = imagePublicIds
        )

        // 2️⃣ Ghi dữ liệu + ép server timestamp
        db.runBatch { batch ->
            batch.set(docRef, post)
            batch.update(
                docRef,
                mapOf(
                    "created_at" to FieldValue.serverTimestamp(),
                    "updated_at" to FieldValue.serverTimestamp()
                )
            )
        }.await()

        // Sau khi ghi post xong
        db.collection("users").document(userId)
            .update("post_count", FieldValue.increment(1))
            .await()

        return docRef.id
    }

    override suspend fun createPostWithCloudinary(
        userId: String,
        caption: String,
        location: Location?,
        imageFiles: List<File>
    ): String = coroutineScope {
        // 1) Upload từng ảnh vào folder 'posts' (bạn có thể thay posts/$userId)
        // Chỗ này cần phải thực thi 1 cách song song, tức là có thể là upload
        // nhiều ảnh cùng 1 lúc
        val results = imageFiles.map { f ->
            async { cloudinary.uploadImage(f, folder = "posts") }
        }.awaitAll()

        val urls = results.map { it.secureUrl }
        val publicIds = results.map { it.publicId }

        // 2) Lưu vào Firestore
        createPost(
            userId = userId,
            caption = caption,
            location = location,
            imageUrls = urls,
            imagePublicIds = publicIds
        )
    }

    override suspend fun getPost(postId: String): Post? {
        val snap = db.collection("posts").document(postId).get().await()
        return snap.toObject(Post::class.java)?.copy(id = snap.id)
    }

    override suspend fun getPostsPaginated(
        pageSize: Int,
        startAfterDoc: DocumentSnapshot?
    ): Pair<List<Post>, DocumentSnapshot?> {
        try {
            var query: Query = db.collection("posts")
                .orderBy("created_at", Query.Direction.DESCENDING)
                .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
                .limit(pageSize.toLong())

            if (startAfterDoc != null) {
                // Start after the last DocumentSnapshot from previous page
                query = query.startAfter(startAfterDoc)
            }

            val snapshot = query.get().await()
            val posts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(id = doc.id)
            }

            // lastVisible used as cursor for next page; null if no results
            val lastVisible: DocumentSnapshot? = if (snapshot.documents.isNotEmpty()) {
                snapshot.documents.last()
            } else {
                null
            }

            return Pair(posts, lastVisible)
        } catch (e: Exception) {
            // Trong trường hợp error, bạn có thể ném exception hoặc trả rỗng
            // Ở đây mình trả Pair(emptyList(), null) — caller cần xử lý
            return Pair(emptyList(), null)
        }
    }

    override suspend fun getAllPosts(): List<Post> {
        return try {
            val snapshot = db.collection("posts")
                .orderBy("created_at", Query.Direction.DESCENDING) // mới → cũ
                .orderBy(
                    FieldPath.documentId(),
                    Query.Direction.DESCENDING
                ) // tránh trùng createdAt
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            // Nếu chưa có trường createdAt thì fallback lấy tất cả
            val snapshot = db.collection("posts").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(id = doc.id)
            }
        }
    }

    override suspend fun getPostsByUser(userId: String): List<Post> {
        val snapshot = db.collection("posts")
            .whereEqualTo("user_id", userId) // tên field trên Firestore
            .orderBy("created_at", Query.Direction.DESCENDING)
            .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING) // tiebreaker đúng
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Post::class.java)?.copy(id = doc.id)
        }
    }

    override suspend fun addComment(postId: String, userId: String, content: String): String {
        val commentsRef = db.collection("posts").document(postId).collection("comments")
        val docRef = commentsRef.document()

        val comment = Comment(
            id = docRef.id,
            userId = userId,
            content = content
        )

        db.runBatch { batch ->
            // Ghi dữ liệu comment
            batch.set(docRef, comment)

            // Cập nhật thời gian tạo trên server
            batch.update(
                docRef,
                mapOf(
                    "created_at" to FieldValue.serverTimestamp(),
                    "updated_at" to FieldValue.serverTimestamp()
                )
            )

            // Tăng count comment trong post cha
            batch.update(
                db.collection("posts").document(postId),
                "counts.comments",
                FieldValue.increment(1)
            )
        }.await()

        return docRef.id
    }

    override suspend fun likePost(postId: String, userId: String) {
        val likeDoc = db.collection("posts").document(postId).collection("likes").document(userId)
        db.runBatch { b ->
            b.set(likeDoc, Like()) // only liked_at (server timestamp in model)
            b.update(
                db.collection("posts").document(postId),
                "counts.likes",
                FieldValue.increment(1)
            )
        }.await()
    }

    override suspend fun getUsersWhoLiked(postId: String): List<User> = coroutineScope {
        // 1) Lấy danh sách userId từ likes
        val likeSnapshot = db.collection("posts")
            .document(postId)
            .collection("likes")
            .get()
            .await()

        val userIds = likeSnapshot.documents.map { it.id }

        // 2) Query từng user theo userId (song song)
        val tasks = userIds.map { uid ->
            async {
                val userSnap = db.collection("users")
                    .document(uid)
                    .get()
                    .await()

                userSnap.toObject(User::class.java)
            }
        }

        // 3) Trả về danh sách user (loại null)
        tasks.awaitAll().filterNotNull()
    }

    override suspend fun deletePost(postId: String, userId: String) {

        val tag = "DeletePost"

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d(tag, "🔥 deletePost() CALLED")
        Log.d(tag, "currentUser = $uid")
        Log.d(tag, "postId = $postId")
        Log.d(tag, "postOwner(userId) = $userId")

        val postRef = db.collection("posts").document(postId)
        val userRef = db.collection("users").document(userId)

        val snapshot = postRef.get().await()
        val post = snapshot.toObject(Post::class.java)

        Log.d(tag, "post exists = ${snapshot.exists()}")
        Log.d(tag, "post.user_id = ${post?.userId}")
        Log.d(tag, "isOwner = ${uid == post?.userId}")

        if (post == null) {
            Log.e(tag, "❌ Post is NULL → STOP")
            return
        }

        // Xóa ảnh Cloudinary
        Log.d(tag, "🖼️ Step1: Deleting Cloudinary images (${post.imagePublicIds.size})")
        if (post.imagePublicIds.isNotEmpty()) {
            coroutineScope {
                post.imagePublicIds.forEach { publicId ->
                    launch(Dispatchers.IO) {
                        try {
                            Log.d(tag, "→ Deleting Cloudinary image: $publicId")
                            cloudinary.deleteImage(publicId)
                            Log.d(tag, "   ✅ Deleted $publicId")
                        } catch (e: Exception) {
                            Log.e(tag, "   ❌ Failed: $publicId → ${e.message}")
                        }
                    }
                }
            }
        }

        // Xóa comments
        val comments = postRef.collection("comments").get().await().documents
        Log.d(tag, "💬 Step2: comments = ${comments.size}")
        comments.forEach {
            Log.d(tag, "→ delete comment: ${it.id}")
            it.reference.delete().await()
        }

        // Xóa likes
        val likes = postRef.collection("likes").get().await().documents
        Log.d(tag, "❤️ Step2: likes = ${likes.size}")
        likes.forEach {
            Log.d(tag, "→ delete like: ${it.id}")
            it.reference.delete().await()
        }

        // Batch delete + update
        Log.d(tag, "🗑️ Step3: Running batch...")
        try {
            db.runBatch { batch ->
                batch.delete(postRef)
                batch.update(userRef, "post_count", FieldValue.increment(-1))
            }.await()

            Log.d(tag, "✅ Batch SUCCESS — Post deleted")
        } catch (e: Exception) {
            Log.e(tag, "❌ Batch FAILED → ${e.message}")
        }
    }


    override suspend fun unlikePost(postId: String, userId: String) {
        val likeDoc = db.collection("posts").document(postId).collection("likes").document(userId)
        db.runBatch { b ->
            b.delete(likeDoc)
            b.update(
                db.collection("posts").document(postId),
                "counts.likes",
                FieldValue.increment(-1)
            )
        }.await()
    }

    override suspend fun hasUserLiked(postId: String, userId: String): Boolean {
        val doc = db.collection("posts").document(postId).collection("likes").document(userId).get()
            .await()
        return doc.exists()
    }

    override suspend fun getComments(postId: String): List<Comment> {
        val snapshot = db.collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("created_at", Query.Direction.DESCENDING) // Mới nhất lên đầu
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Comment::class.java)?.copy(id = doc.id)
        }
    }

    override suspend fun deleteComment(postId: String, commentId: String) {
        val postRef = db.collection("posts").document(postId)
        val commentRef = postRef.collection("comments").document(commentId)

        db.runBatch { batch ->
            batch.delete(commentRef)
            batch.update(postRef, "counts.comments", FieldValue.increment(-1))
        }.await()
    }

    override suspend fun updatePost(
        existingPost: Post,
        removeImageUrls: List<String>,
        newCaption: String?
    ) {
        val tag = "UpdatePost"

        val postRef = db.collection("posts").document(existingPost.id)

        Log.d(tag, "🔥 updatePost() CALLED")
        Log.d(tag, "existingPost.id = ${existingPost.id}")
        Log.d(tag, "removeImageUrls = $removeImageUrls")
        Log.d(tag, "newCaption = $newCaption")


        // Chuyển URL → publicId (trùng nhau đoạn "posts/xxxxx")
        val removePublicIds = removeImageUrls.mapNotNull { url ->
            // URL có dạng .../posts/abc123.jpg
            val regex = "posts/([a-zA-Z0-9_-]+)".toRegex()
            val match = regex.find(url)
            match?.value // kết quả: "posts/abc123"
        }

        Log.d(tag, "👉 removePublicIds = $removePublicIds")

        // Lọc danh sách mới sau khi loại bỏ ảnh
        val newImageUrls = existingPost.imageUrls.filterNot { it in removeImageUrls }
        val newImagePublicIds = existingPost.imagePublicIds.filterNot { pid ->
            removePublicIds.contains(pid)
        }

        // Không cho xoá hết ảnh
        if (newImageUrls.isEmpty()) {
            Log.e(tag, "❌ Cannot remove all images. Post must have at least ONE image.")
            return
        }

        // xóa ảnh trên Cloudinary
        if (removePublicIds.isNotEmpty()) {
            coroutineScope {
                removePublicIds.forEach { publicId ->
                    launch(Dispatchers.IO) {
                        try {
                            Log.d(tag, "🗑️ Deleting Cloudinary image: $publicId")
                            cloudinary.deleteImage(publicId)
                            Log.d(tag, "   ✅ Deleted $publicId")
                        } catch (e: Exception) {
                            Log.e(tag, "   ❌ Failed to delete $publicId → ${e.message}")
                        }
                    }
                }
            }
        }

        // Tạo Post sau khi mà chỉnh sửa
        val updatedPost = existingPost.copy(
            caption = newCaption ?: existingPost.caption,
            imageUrls = newImageUrls,
            imagePublicIds = newImagePublicIds
        )

        Log.d(tag, "🆕 updatedPost = $updatedPost")

        // lưu lên trên Firestore
        try {
            db.runBatch { batch ->
                batch.set(postRef, updatedPost)
                batch.update(postRef, "updated_at", FieldValue.serverTimestamp())
            }.await()

            Log.d(tag, "✅ Post updated successfully")
        } catch (e: Exception) {
            Log.e(tag, "❌ Firestore update FAILED → ${e.message}")
        }
    }
}
