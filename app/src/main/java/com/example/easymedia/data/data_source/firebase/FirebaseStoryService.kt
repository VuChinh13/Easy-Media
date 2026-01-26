package com.example.easymedia.data.data_source.firebase

import android.util.Log
import com.example.easymedia.data.data_source.cloudinary.CloudinaryService
import com.example.easymedia.data.data_source.cloudinary.CloudinaryUploadResult
import com.example.easymedia.data.model.Music
import com.example.easymedia.data.model.Story
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.io.File
import java.sql.Date

interface StoryService {
    /**
     * Lấy tất cả các bài hát trong collection "musics"
     * @return List<Music>
     */
    suspend fun getAllMusics(): List<Music>

    /**
     * Upload story mới lên Firestore
     * @param story: Storyy — chứa thông tin story (imageUrl sẽ được cập nhật sau khi upload)
     * @param imageFile: File — ảnh story được chụp
     * @return Boolean — true nếu upload thành công
     */
    suspend fun uploadStory(story: Story, imageFile: File, isVideo: Boolean): Boolean

    /**
     * Lấy tất cả story từ collection "stories"
     * @return List<Story>
     */
    suspend fun getAllStories(): List<Story>
    suspend fun deleteStory(storyId: String): Boolean
    suspend fun getStoriesByUser(userId: String): List<Story>
}

class FirebaseStoryService(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val cloudinary: CloudinaryService
) : StoryService {

    override suspend fun getAllMusics(): List<Music> {
        return try {
            Log.d("FirebaseStoryService", "Start fetching musics...")
            val snapshot = db.collection("musics")
                .get()
                .await()
            Log.d("FirebaseStoryService", "Snapshot fetched, size = ${snapshot.size()}")
            val musics = snapshot.documents.mapNotNull { doc ->
                val title = doc.getString("title") ?: ""
                val artist = doc.getString("artist") ?: ""
                val url = doc.getString("url") ?: ""
                val duration = doc.getString("duration") ?: ""
                val publicId = doc.getString("publicId") ?: ""
                val image = doc.getString("image") ?: ""
                Music(
                    title = title,
                    artist = artist,
                    url = url,
                    publicId = publicId,
                    duration = duration,
                    image = image
                )
            }
            musics
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun uploadStory(story: Story, imageFile: File, isVideo: Boolean): Boolean {
        return try {
            var result: CloudinaryUploadResult? = null
            if (isVideo) {
                result = cloudinary.uploadVideo(imageFile, folder = "stories")
                Log.d("FirebaseStoryService", "Uploaded to Cloudinary: ${result.secureUrl}")
            } else {
                result = cloudinary.uploadImage(imageFile, folder = "stories")
                Log.d("FirebaseStoryService", "Uploaded to Cloudinary: ${result.secureUrl}")
            }

            val docRef = db.collection("stories").document()

            val storyMap = mutableMapOf(
                "user_id" to story.userId,
                "image_url" to result.secureUrl,
                "expire_at" to Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000),
                if (isVideo) "thumbnail_url" to generateVideoThumbnail(result.secureUrl) else "thumbnail_url" to result.secureUrl,
                "created_at" to FieldValue.serverTimestamp(),
                "duration_ms" to story.durationMs
            )

            story.music?.let { music ->
                storyMap["music"] = mapOf(
                    "title" to music.title,
                    "artist" to music.artist,
                    "url" to music.url,
                    "publicId" to music.publicId,
                    "duration" to music.duration
                )
            }

            docRef.set(storyMap).await()
            Log.d("FirebaseStoryService", "Story uploaded successfully with ID: ${docRef.id}")
            true
        } catch (e: Exception) {
            Log.e("FirebaseStoryService", "Failed to upload story: ${e.message}", e)
            false
        }
    }

    override suspend fun getAllStories(): List<Story> {
        return try {
            Log.d("FirebaseStoryService", "Start fetching stories...")
            val snapshot = db.collection("stories")
                .orderBy("created_at", Query.Direction.DESCENDING) // mới → cũ
                .orderBy(
                    FieldPath.documentId(),
                    Query.Direction.DESCENDING
                )
                .get()
                .await()

            Log.d("FirebaseStoryService", "Snapshot fetched, size = ${snapshot.size()}")
            val stories = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Story::class.java)?.copy(id = doc.id)
            }

            Log.d("FirebaseStoryService", "Fetched stories: ${stories.size} items")
            stories
        } catch (e: Exception) {
            Log.w(
                "FirebaseStoryService",
                "Failed ordered fetch, falling back to simple fetch: ${e.message}",
                e
            )
            return try {
                val snapshot = db.collection("stories").get().await()
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Story::class.java)?.copy(id = doc.id)
                }
            } catch (inner: Exception) {
                Log.e("FirebaseStoryService", "Failed to fetch stories: ${inner.message}", inner)
                emptyList()
            }
        }
    }

    fun generateVideoThumbnail(videoUrl: String): String {
        if (videoUrl.isBlank()) return videoUrl
        if (!videoUrl.contains("/video/upload/")) return videoUrl
        return videoUrl.replace("/upload/", "/upload/so_0,f_jpg,w_500,h_900,c_fill/")
            .replace(".mp4", ".jpg")
    }

    override suspend fun deleteStory(storyId: String): Boolean {
        val tag = "DeleteStory"
        return try {
            val docRef = db.collection("stories").document(storyId)
            val snapshot = docRef.get().await()

            if (!snapshot.exists()) {
                return false
            }

            val imageUrl = snapshot.getString("image_url") ?: ""

            if (imageUrl.isEmpty()) {
                Log.e(tag, "Story has no imageUrl → Cannot delete Cloudinary")
            } else {
                val publicId = extractPublicId(imageUrl)
                Log.d(tag, "Extracted publicId = $publicId")

                if (publicId != null) {
                    val isVideo = isVideoUrl(imageUrl)

                    if (isVideo) {
                        Log.d(tag, "Detected VIDEO → Deleting Cloudinary video")
                        cloudinary.deleteVideo(publicId)
                    } else {
                        Log.d(tag, "Detected IMAGE → Deleting Cloudinary image")
                        cloudinary.deleteImage(publicId)
                    }
                } else {
                    Log.e(tag, "Failed to extract publicId → Skip Cloudinary delete")
                }
            }

            docRef.delete().await()
            Log.d(tag, "Story deleted successfully")

            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete story → ${e.message}", e)
            false
        }
    }

    private fun extractPublicId(url: String): String? {
        // URL dạng: .../stories/abcd1234.jpg
        val regex = "stories/([a-zA-Z0-9_-]+)".toRegex()
        val match = regex.find(url)
        return match?.value // stories/abcd1234
    }


    private fun isVideoUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.endsWith(".mp4") ||
                lower.endsWith(".mov") ||
                lower.endsWith(".mkv") ||
                lower.endsWith(".avi")
    }

    override suspend fun getStoriesByUser(userId: String): List<Story> {
        Log.d("FirebaseStoryService", "Fetching stories for user: $userId")

        return try {
            val snapshot = db.collection("stories")
                .whereEqualTo("user_id", userId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
                .get()
                .await()

            val stories = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Story::class.java)?.copy(id = doc.id)
            }

            Log.d("FirebaseStoryService", "Fetched ${stories.size} stories for user $userId")
            stories

        } catch (e: Exception) {
            Log.w(
                "FirebaseStoryService",
                "Ordered fetch failed for user $userId → fallback: ${e.message}",
                e
            )
            return try {
                val snapshot = db.collection("stories")
                    .whereEqualTo("user_id", userId)
                    .get()
                    .await()

                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Story::class.java)?.copy(id = doc.id)
                }
                    .sortedByDescending { it.createdAt }

            } catch (inner: Exception) {
                Log.e(
                    "FirebaseStoryService",
                    "Failed to fetch user stories: ${inner.message}",
                    inner
                )
                emptyList()
            }
        }
    }

}
