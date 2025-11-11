package com.example.easymedia.ui.component.story

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easymedia.data.data_source.firebase.FirebaseStoryService
import com.example.easymedia.data.model.Music
import com.example.easymedia.data.repository.StoryRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StoryViewModel : ViewModel(){
    private  val storyRepository = StoryRepositoryImpl(FirebaseStoryService())
    private val _listMusic  =  MutableLiveData<List<Music>>()
    val listMusic  = _listMusic

    fun getAllMusic(){
        viewModelScope.launch (Dispatchers.IO){
            val result = storyRepository.getAllMusics()
            if (result.isNotEmpty()){
                _listMusic.postValue(result)
            }
        }
    }
}