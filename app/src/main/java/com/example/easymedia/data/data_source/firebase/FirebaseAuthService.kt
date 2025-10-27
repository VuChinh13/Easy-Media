package com.example.easymedia.data.data_source.firebase

import android.util.Log
import com.example.easymedia.data.data_source.cloudinary.CloudinaryService
import com.example.easymedia.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.File

interface AuthService {
    suspend fun isUsernameAvailable(usernameLower: String): Boolean
    suspend fun mapUsernameToUid(usernameLower: String, uid: String)
    suspend fun createAuthUser(email: String, password: String): String
    suspend fun createUserProfile(uid: String, user: User)
    suspend fun sendEmailVerification()
    suspend fun signIn(email: String, password: String): String
    suspend fun getUserById(uid: String): User?
    fun signOut()
    suspend fun getUsersByIds(uids: List<String>): List<User>
    fun currentUid(): String?
    suspend fun updateUserProfile(
        uid: String,
        fullName: String?,
        bio: String?,
        location: String?,
        profilePicture: File?,
        gender: String?
    )
}

class FirebaseAuthService(private val cloudinary: CloudinaryService) : AuthService {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Tên người dùng đã tồn tại thì return false - ngược lại là false
    // Cần có bảng usernames để truy vấn người dùng
    override suspend fun isUsernameAvailable(usernameLower: String): Boolean {
        val doc = db.collection("usernames").document(usernameLower).get().await()
        return !doc.exists()
    }

    override suspend fun mapUsernameToUid(usernameLower: String, uid: String) {
        db.collection("usernames").document(usernameLower)
            .set(mapOf("uid" to uid))
            .await()
    }

    override suspend fun createAuthUser(email: String, password: String): String {
        val user = auth.createUserWithEmailAndPassword(email, password).await().user
            ?: error("Create user failed")
        return user.uid
    }

    override suspend fun createUserProfile(uid: String, user: User) {
        db.collection("users").document(uid).set(user.copy(id = uid)).await()
    }

    override suspend fun sendEmailVerification() {
        auth.currentUser?.sendEmailVerification()?.await()
    }

    override suspend fun signIn(email: String, password: String): String {
        val u = auth.signInWithEmailAndPassword(email, password).await().user
            ?: error("Sign in failed")
        return u.uid
    }

    override suspend fun getUserById(uid: String): User? {
        val snapshot = db.collection("users")
            .document(uid)
            .get()
            .await()

        return snapshot.toObject(User::class.java)
    }

    override fun signOut() = auth.signOut()

    override suspend fun getUsersByIds(uids: List<String>): List<User> {
        if (uids.isEmpty()) return emptyList()

        val result = mutableListOf<User>()
        // Chia nhỏ danh sách nếu > 10 id
        val chunks = uids.chunked(10)

        for (chunk in chunks) {
            val snapshot = db.collection("users")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                .get()
                .await()

            val users = snapshot.toObjects(User::class.java)
            result.addAll(users)
        }

        return result
    }

    override suspend fun updateUserProfile(
        uid: String,
        fullName: String?,
        bio: String?,
        location: String?,
        profilePicture: File?, // 🔹 đổi từ String? → File?
        gender: String?
    ) {
        val updates = mutableMapOf<String, Any?>()

        fullName?.let { updates["full_name"] = it }
        bio?.let { updates["bio"] = it }
        location?.let { updates["location"] = it }
        gender?.let { updates["gender"] = it }

        // Cập nhật thời gian
        updates["updated_at"] = FieldValue.serverTimestamp()

        // 🔹 Nếu có ảnh mới → upload ảnh lên Cloudinary
        if (profilePicture != null) {
            val userRef = db.collection("users").document(uid)

            // Lấy public_id cũ (nếu có) để xóa sau
            val currentData = userRef.get().await().data
            val oldPublicId = currentData?.get("profile_picture_public_id") as? String

            // Upload ảnh mới
            val uploadResult = cloudinary.uploadImage(profilePicture, folder = "profiles/$uid")
            updates["profile_picture"] = uploadResult.secureUrl
            updates["profile_picture_public_id"] = uploadResult.publicId

            // Cập nhật Firestore trước
            userRef.update(updates).await()

            // 🔹 Sau đó xóa ảnh cũ (nếu có)
            if (!oldPublicId.isNullOrEmpty()) {
                try {
                    val success = cloudinary.deleteImage(oldPublicId)
                    if (!success) {
                        Log.w(
                            "FirebaseUserService",
                            "Failed to delete old Cloudinary image: $oldPublicId"
                        )
                    }
                } catch (e: Exception) {
                    Log.w("FirebaseUserService", "Error deleting old image: ${e.message}")
                }
            }
        } else {
            // 🔹 Không có ảnh mới → chỉ update text fields
            db.collection("users").document(uid).update(updates).await()
        }
    }

    override fun currentUid(): String? = auth.currentUser?.uid
}
