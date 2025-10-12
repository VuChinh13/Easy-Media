package com.example.instagram.data.data_source.firebase

import com.example.instagram.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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
}

class FirebaseAuthService : AuthService {

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

    override fun currentUid(): String? = auth.currentUser?.uid
}
