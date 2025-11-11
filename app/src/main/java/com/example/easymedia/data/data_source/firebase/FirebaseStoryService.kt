package com.example.easymedia.data.data_source.firebase

import android.util.Log
import com.example.easymedia.data.model.Music
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

interface StoryService {
    /**
     * Lấy tất cả các bài hát trong collection "musics"
     * @return List<Music>
     */
    suspend fun getAllMusics(): List<Music>
}

class FirebaseStoryService(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
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
}
