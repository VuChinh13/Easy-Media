package com.example.easymedia.data.data_source.firebase

import android.util.Log
import com.example.easymedia.data.data_source.cloudinary.CloudinaryService
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
    suspend fun uploadStory(story: Story, imageFile: File): Boolean

    /**
     * Lấy tất cả story từ collection "stories"
     * @return List<Story>
     */
    suspend fun getAllStories(): List<Story>
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
                Music(
                    title = title,
                    artist = artist,
                    url = url,
                    publicId = publicId,
                    duration = duration
                )
            }

            // ✅ Log toàn bộ danh sách
            Log.d("FirebaseStoryService", "Fetched musics: $musics")

            musics
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun uploadStory(story: Story, imageFile: File): Boolean {
        return try {
            Log.d("FirebaseStoryService", "Uploading story image to Cloudinary...")

            // 1️⃣ Upload ảnh lên Cloudinary
            val result = cloudinary.uploadImage(imageFile, folder = "stories")
            Log.d("FirebaseStoryService", "Uploaded to Cloudinary: ${result.secureUrl}")

            // 2️⃣ Tạo document reference Firestore (ID tự sinh)
            val docRef = db.collection("stories").document()

            // 3️⃣ Tạo map dữ liệu + convert Music sang Map
            val storyMap = mutableMapOf<String, Any>(
                "user_id" to story.userId,
                "image_url" to result.secureUrl,
                "expire_at" to Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000),
                "viewers" to story.viewers,
                "created_at" to FieldValue.serverTimestamp()
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

            // 4️⃣ Ghi dữ liệu 1 lần
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
                ) // tránh trùng createdAt
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
}
