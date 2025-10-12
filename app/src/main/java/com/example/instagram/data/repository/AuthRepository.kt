package com.example.instagram.data.repository

import com.example.instagram.data.data_source.firebase.AuthService
import com.example.instagram.data.model.User

sealed class AuthError : Throwable() {
    object UsernameTaken : AuthError()
    data class Firebase(val code: String?, override val message: String?) : AuthError()
}

interface AuthRepository {
    suspend fun signup(
        email: String,
        password: String,
        username: String,
        fullName: String
    ): Result<String> // uid

    suspend fun login(email: String, password: String): Result<String>
    fun logout()
    fun currentUid(): String?

    suspend fun getUserById(uid: String): Result<User?>

    suspend fun getUsersByIds(uids: List<String>): List<User>
}

class AuthRepositoryImpl(
    private val service: AuthService
) : AuthRepository {

    override suspend fun signup(
        email: String,
        password: String,
        username: String,
        fullName: String
    ): Result<String> = runCatching {
        val usernameLower = username.lowercase()

        // 1) Check username
        val ok = service.isUsernameAvailable(usernameLower)
        if (!ok) throw AuthError.UsernameTaken

        // 2) Create Auth user -> uid
        val uid = service.createAuthUser(email, password)

        // 3) Map username -> uid (đảm bảo duy nhất)
        service.mapUsernameToUid(usernameLower, uid)

        // 4) Create user profile
        val profile = User(
            id = uid,
            username = username,
            fullName = fullName,
            email = email,
            profilePicture = null,
            bio = "",
            location = null,
            followers = emptyList(),
            following = emptyList(),
            postCount = 0,
            createdAt = null, // Firestore sẽ tự gán @ServerTimestamp nếu bạn để kiểu Date trong model
            updatedAt = null
        )
        service.createUserProfile(uid, profile)

        uid
    }.recoverCatching { e ->
        // TODO: bọc lại lỗi trả về từ firebase nếu muốn
        //  trường hợp này thì không bọc và trả về nguyên bản lỗi mà trả về từ Firebase
        throw e
    }

    override suspend fun login(email: String, password: String): Result<String> =
        runCatching { service.signIn(email, password) }

    override fun logout() = service.signOut()

    override fun currentUid(): String? = service.currentUid()
    override suspend fun getUserById(uid: String): Result<User?> =
        runCatching { service.getUserById(uid) }

    override suspend fun getUsersByIds(uids: List<String>): List<User> = service.getUsersByIds(uids)
}
