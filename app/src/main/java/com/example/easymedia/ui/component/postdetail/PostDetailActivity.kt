//package com.example.instagram.ui.component.postdetail
//
//import android.os.Bundle
//import androidx.activity.viewModels
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.example.instagram.databinding.ActivityPostDetailBinding
//import com.example.instagram.ui.component.postdetail.adapter.PostDetailAdapter
//import com.example.instagram.ui.component.utils.SharedPrefer
//
//
//// Class này sẽ nhận 1 tham số là index -> vị trí click bên Fragment khác
//class PostDetailActivity : AppCompatActivity() {
//    private lateinit var binding: ActivityPostDetailBinding
//    private val myProfileViewModel: PostDetailViewModel by viewModels()
//
//    private lateinit var postDetailAdapter: PostDetailAdapter
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityPostDetailBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        SharedPrefer.updateContext(this)
//        val userName = SharedPrefer.getUserName()
//        myProfileViewModel.getInforUser(userName)
//        myProfileViewModel.getUserPosts(userName)
//
//
//        myProfileViewModel.getUserPostsResult.observe(this) { result ->
//            if (result != null) {
//                SharedPrefer.updateContext(this)
//                val userId = SharedPrefer.getUserId()
//                val userName = SharedPrefer.getUserName()
//                postDetailAdapter =
//                    PostDetailAdapter(this, result.data.data, userName, userId)
//                binding.rvMyPost.layoutManager = LinearLayoutManager(
//                    this,
//                    LinearLayoutManager.VERTICAL, false
//                )
//                binding.rvMyPost.adapter = postDetailAdapter
//            }
//        }
//
//        binding.ivBackArrow.setOnClickListener {
//            finish()
//        }
//    }
//
//}