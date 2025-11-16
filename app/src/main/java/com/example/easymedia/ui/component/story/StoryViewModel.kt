package com.example.easymedia.ui.component.story

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.data.data_source.firebase.FirebaseStoryService
import com.example.easymedia.data.model.Music
import com.example.easymedia.data.model.Story
import com.example.easymedia.data.repository.StoryRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class StoryViewModel : ViewModel() {
    private val storyRepository =
        StoryRepositoryImpl(FirebaseStoryService(cloudinary = CloudinaryServiceImpl()))
    private val _listMusic = MutableLiveData<List<Music>>()
    val listMusic: LiveData<List<Music>> = _listMusic
    private val _finish = MutableLiveData<Boolean>()
    val finish: LiveData<Boolean> = _finish

    fun getAllMusic() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = storyRepository.getAllMusics()
            if (result.isNotEmpty()) {
                _listMusic.postValue(result)
            }
        }
    }

    fun uploadStory(story: Story, imageFile: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = storyRepository.uploadStory(story, imageFile,false)
            _finish.postValue(result)
        }
    }
}