package com.example.easymedia.data.data_source.firebase

import android.util.Log
import com.example.easymedia.data.data_source.cloudinary.CloudinaryService
import com.example.easymedia.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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
    suspend fun getAllUsers(): List<User>
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

    suspend fun addFollowing(currentUid: String, targetUid: String)

    suspend fun removeFollowing(currentUid: String, targetUid: String)
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
        profilePicture: File?, // File local
        gender: String?
    ) {
        withContext(Dispatchers.IO) { // đảm bảo I/O chạy trên thread nền
            val userRef = db.collection("users").document(uid)
            val updates = mutableMapOf<String, Any?>()

            fullName?.let { updates["full_name"] = it }
            bio?.let { updates["bio"] = it }
            location?.let { updates["location"] = it }
            gender?.let { updates["gender"] = it }
            updates["updated_at"] = FieldValue.serverTimestamp()

            if (profilePicture != null) {
                // Lấy dữ liệu cũ (đồng bộ Firestore)
                val currentData = userRef.get().await().data
                val oldPublicId = currentData?.get("profile_picture_public_id") as? String

                // Upload ảnh mới (I/O)
                val uploadResult = cloudinary.uploadImage(profilePicture, folder = "profiles/$uid")

                // Cập nhật đường dẫn ảnh mới
                updates["profile_picture"] = uploadResult.secureUrl
                updates["profile_picture_public_id"] = uploadResult.publicId

                // Cập nhật Firestore
                userRef.update(updates).await()

                // Xóa ảnh cũ — KHÔNG chờ (song song)
                if (!oldPublicId.isNullOrEmpty()) {
                    launch(Dispatchers.IO) {
                        try {
                            val success = cloudinary.deleteImage(oldPublicId)
                            if (!success) {
                                Log.w(
                                    "FirebaseStoryService",
                                    "Failed to delete old Cloudinary image: $oldPublicId"
                                )
                            }
                        } catch (e: Exception) {
                            Log.w("FirebaseStoryService", "Error deleting old image: ${e.message}")
                        }
                    }
                }
            } else {
                // Không có ảnh mới → chỉ update thông tin text
                userRef.update(updates).await()
            }
        }
    }

    override suspend fun getAllUsers(): List<User> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = db.collection("users").get().await()
                snapshot.toObjects(User::class.java)
            } catch (e: Exception) {
                Log.e("FirebaseAuthService", "Error getting all users: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun addFollowing(currentUid: String, targetUid: String) {
        Log.d("FollowDebug", "addFollowing() called: currentUid=$currentUid, targetUid=$targetUid")

        if (currentUid == targetUid) {
            Log.e("FollowDebug", "❌ Không thể tự follow chính mình!")
            return
        }

        try {
            val batch = db.batch()
            val currentRef = db.collection("users").document(currentUid)
            val targetRef = db.collection("users").document(targetUid)

            Log.d("FollowDebug", "Updating Firestore...")

            batch.update(currentRef, "following", FieldValue.arrayUnion(targetUid))
            batch.update(targetRef, "followers", FieldValue.arrayUnion(currentUid))

            batch.commit().await()

            Log.d("FollowDebug", "✅ Follow thành công: $currentUid → $targetUid")

        } catch (e: Exception) {
            Log.e("FollowDebug", "❌ Lỗi khi follow: ${e.message}", e)
        }
    }

    override fun currentUid(): String? = auth.currentUser?.uid

    override suspend fun removeFollowing(currentUid: String, targetUid: String) {
        Log.d("FollowDebug", "removeFollowing() called: currentUid=$currentUid, targetUid=$targetUid")

        if (currentUid == targetUid) {
            Log.e("FollowDebug", "❌ Không thể tự unfollow chính mình!")
            return
        }

        try {
            val batch = db.batch()
            val currentRef = db.collection("users").document(currentUid)
            val targetRef = db.collection("users").document(targetUid)

            Log.d("FollowDebug", "Updating Firestore...")

            batch.update(currentRef, "following", FieldValue.arrayRemove(targetUid))
            batch.update(targetRef, "followers", FieldValue.arrayRemove(currentUid))

            batch.commit().await()

            Log.d("FollowDebug", "✅ Unfollow thành công: $currentUid → $targetUid")

        } catch (e: Exception) {
            Log.e("FollowDebug", "❌ Lỗi khi unfollow: ${e.message}", e)
        }
    }
}
