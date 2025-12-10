package com.example.easymedia.ui.component.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.data.data_source.firebase.FirebasePostService
import com.example.easymedia.data.data_source.firebase.FirebaseStoryService
import com.example.easymedia.data.model.Post
import com.example.easymedia.data.model.Story
import com.example.easymedia.data.repository.PostRepository
import com.example.easymedia.data.repository.PostRepositoryImpl
import com.example.easymedia.data.repository.StoryRepositoryImpl
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class HomeViewModel : ViewModel() {
    private val repoPost: PostRepository =
        PostRepositoryImpl(FirebasePostService(cloudinary = CloudinaryServiceImpl()))

    private val _posts = MutableLiveData<Pair<List<Post>, String>>()
    val posts: LiveData<Pair<List<Post>, String>> = _posts
    private val _story = MutableLiveData<List<Story>>()
    val story: LiveData<List<Story>> = _story
    private var lastVisible: DocumentSnapshot? = null
    private var isLoading = false
    private var isLastPage = false
    private val currentPosts = mutableListOf<Post>()
    private val pageSize = 10
    private val storyRepository =
        StoryRepositoryImpl(FirebaseStoryService(cloudinary = CloudinaryServiceImpl()))

    fun fetchFirstPage() {
        if (isLoading) return
        isLoading = true
        isLastPage = false

        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                repoPost.fetchFirstPage(pageSize)
            }

            result.onSuccess { (list, lastDoc) ->
                currentPosts.clear()
                currentPosts.addAll(list)
                lastVisible = lastDoc
                _posts.postValue(currentPosts to "")
                isLastPage = list.isEmpty()
            }.onFailure { e ->
                _posts.postValue(emptyList<Post>() to (e.message ?: "Đã có lỗi xảy ra"))
                Log.e("HomeVM", "Error: ${e.message}")
            }

            isLoading = false
        }
    }

//    fun getAllStories() {
//        viewModelScope.launch(Dispatchers.IO) {
//            val result = storyRepository.getAllStories()
//            if (result.isNotEmpty()) {
//                _story.postValue(result)
//            }
//            Log.d("abcd", result.size.toString())
//        }
//    }

    fun getAllStories() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = storyRepository.getAllStories()

            // Thời gian hiện tại
            val now = Date()

            // Lọc story chưa hết hạn
            val filteredStories = result.filter { story ->
                story.expireAt?.after(now) == true
            }

            if (filteredStories.isNotEmpty()) {
                _story.postValue(filteredStories)
            } else {
                _story.postValue(emptyList())
            }

            Log.d("Story_Filter", "Total: ${result.size}, Alive: ${filteredStories.size}")
        }
    }

    fun fetchNextPage() {
        if (isLoading || isLastPage || lastVisible == null) return
        isLoading = true

        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                repoPost.fetchNextPage(pageSize, lastVisible)
            }

            result.onSuccess { (newPosts, lastDoc) ->
                if (newPosts.isNotEmpty()) {
                    currentPosts.addAll(newPosts)
                    lastVisible = lastDoc

                    // 👉 CHỈ post danh sách mới vừa load
                    _posts.postValue(newPosts to "")
                } else {
                    isLastPage = true
                }
            }.onFailure { e ->
                Log.e("HomeVM", "Error next page: ${e.message}")
            }

            isLoading = false
        }
    }

    fun refresh() {
        isLoading = false
        isLastPage = false
        lastVisible = null
        currentPosts.clear()
        fetchFirstPage()
    }
}
