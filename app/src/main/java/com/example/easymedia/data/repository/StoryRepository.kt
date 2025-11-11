package com.example.easymedia.data.repository

import com.example.easymedia.data.data_source.firebase.StoryService
import com.example.easymedia.data.model.Music

interface StoryRepository {
    suspend fun getAllMusics(): List<Music>
}

class StoryRepositoryImpl(private val service: StoryService) : StoryRepository {
    override suspend fun getAllMusics(): List<Music> {
        return service.getAllMusics()
    }
}