//package com.example.instagram.ui.component.search
//
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.instagram.data.model.User
//import com.example.instagram.data.repository.AuthRepository
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.async
//import kotlinx.coroutines.awaitAll
//import kotlinx.coroutines.coroutineScope
//import kotlinx.coroutines.launch
//
//class SearchViewModel : ViewModel() {
//    private val authRepository = AuthRepository()
//
//    // Expose List<User> (không để MutableList ở public)
//    private val _getInforUserResults = MutableLiveData<List<User>>(emptyList())
//    val getInforUserResults: LiveData<List<User>> = _getInforUserResults
//
//    fun getListUser() {
//        viewModelScope.launch {
//            // 1) Lấy tổng (fallback 10 nếu null)
//            val totalPosts = authRepository.getPost("moi-nhat", 1, 10)
//                ?.data?.totalPost ?: 10
//
//            // 2) Lấy đầy đủ posts theo total
//            val postsResp = authRepository.getPost("moi-nhat", 1, totalPosts)
//            val posts = postsResp?.data?.data.orEmpty()
//
//            // 3) Rút ra danh sách username (loại null và trùng)
//            val usernames = posts.mapNotNull { it.author?.username }.distinct()
//
//            //  4) Gọi song song để lấy info user, lọc null
//            val users: List<User> = coroutineScope {
//                usernames.map { uname ->
//                    async(Dispatchers.IO) {
//                        authRepository.getInforUser(uname)?.data
//                    }
//                }.awaitAll().filterNotNull()
//            }
//            _getInforUserResults.value = users
//
//        }
//    }
//}
