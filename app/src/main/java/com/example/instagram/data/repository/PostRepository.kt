package com.example.instagram.data.repository

import com.example.instagram.data.data_source.firebase.PostService
import com.example.instagram.data.model.Comment
import com.example.instagram.data.model.Post
import java.io.File

sealed class PostError : Throwable() {
    object NotFound : PostError()
    data class Firebase(val code: String?, override val message: String?) : PostError()
}

interface PostRepository {
    suspend fun createPostWithCloudinary(
        userId: String,
        caption: String,
        location: String?,
        imageFiles: List<File>
    ): Result<String>

    suspend fun getPost(postId: String): Result<Post>

    suspend fun getPostsByUser(userId: String): Result<List<Post>>
    suspend fun addComment(postId: String, userId: String, content: String): Result<String>
    suspend fun likePost(postId: String, userId: String): Result<Unit>
    suspend fun unlikePost(postId: String, userId: String): Result<Unit>
    suspend fun hasUserLiked(postId: String, userId: String): Result<Boolean>
    suspend fun getAllPosts(): Result<List<Post>>
    suspend fun getComments(postId: String): Result<List<Comment>>
}

class PostRepositoryImpl(
    private val service: PostService
) : PostRepository {
    // nếu mà thành công thì trả về
    override suspend fun createPostWithCloudinary(
        userId: String,
        caption: String,
        location: String?,
        imageFiles: List<File>
    ): Result<String> = runCatching {
        service.createPostWithCloudinary(userId, caption, location, imageFiles)
    }

    override suspend fun getPost(postId: String): Result<Post> = runCatching {
        service.getPost(postId) ?: throw PostError.NotFound
    }

    override suspend fun getPostsByUser(userId: String): Result<List<Post>> = runCatching {
        service.getPostsByUser(userId)
    }

    override suspend fun addComment(
        postId: String,
        userId: String,
        content: String
    ): Result<String> =
        runCatching { service.addComment(postId, userId, content) }

    override suspend fun likePost(postId: String, userId: String): Result<Unit> =
        runCatching { service.likePost(postId, userId) }

    override suspend fun unlikePost(postId: String, userId: String): Result<Unit> =
        runCatching { service.unlikePost(postId, userId) }

    override suspend fun hasUserLiked(postId: String, userId: String): Result<Boolean> =
        runCatching { service.hasUserLiked(postId, userId) }

    override suspend fun getAllPosts(): Result<List<Post>> =
        runCatching { service.getAllPosts() }

    override suspend fun getComments(postId: String): Result<List<Comment>> =
        runCatching { service.getComments(postId) }
}
