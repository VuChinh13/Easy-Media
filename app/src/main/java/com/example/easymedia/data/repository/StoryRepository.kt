package com.example.easymedia.data.repository

import com.example.easymedia.data.data_source.firebase.StoryService
import com.example.easymedia.data.model.Music
import com.example.easymedia.data.model.Story
import java.io.File

interface StoryRepository {
    suspend fun getAllMusics(): List<Music>
    suspend fun uploadStory(story: Story, imageFile: File, isVideo: Boolean): Boolean
    suspend fun getAllStories(): List<Story>
    suspend fun deleteStory(storyId: String): Boolean
}

class StoryRepositoryImpl(private val service: StoryService) : StoryRepository {
    override suspend fun getAllMusics(): List<Music> {
        return service.getAllMusics()
    }

    override suspend fun uploadStory(
        story: Story,
        imageFile: File,
        isVideo: Boolean
    ): Boolean {
        return service.uploadStory(story, imageFile, isVideo)
    }

    override suspend fun getAllStories(): List<Story> {
        return service.getAllStories()
    }

    override suspend fun deleteStory(storyId: String): Boolean {
        return service.deleteStory(storyId)
    }
}