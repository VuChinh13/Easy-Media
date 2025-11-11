package com.example.easymedia.ui.component.postdetail

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.easymedia.data.model.User
import com.example.easymedia.databinding.ActivityPostDetailBinding
import com.example.easymedia.ui.component.postdetail.adapter.PostDetailAdapter
import com.example.easymedia.ui.component.utils.IntentExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


// Class này sẽ nhận 1 tham số là index -> vị trí click bên Fragment khác
// hiển thị đúng với vị trí index đó
@Suppress("DEPRECATION")
class PostDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPostDetailBinding
    private val myProfileViewModel: PostDetailViewModel by viewModels()
    private lateinit var postDetailAdapter: PostDetailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showLoading()
        // Nhận dữ liệu
        val user = intent.getParcelableExtra<User>(IntentExtras.EXTRA_USER)
        val position = intent.getIntExtra(IntentExtras.EXTRA_POSITION, 0)
        postDetailAdapter = PostDetailAdapter(this, mutableListOf(), user, lifecycleScope)
        binding.rvMyPost
            .apply {
                layoutManager =
                    LinearLayoutManager(
                        this@PostDetailActivity,
                        LinearLayoutManager.VERTICAL,
                        false
                    )
                setHasFixedSize(true)
                adapter = postDetailAdapter
            }

        myProfileViewModel.getUserPosts(user!!.id)
        // Quan sát dữ liệu bài viết
        myProfileViewModel.getUserPostsResult.observe(this) { data ->
            if (data.isNotEmpty()) {
                // Gộp bài viết mới và bài viết cũ
                postDetailAdapter.addPosts(data)
                binding.rvMyPost.post {
                    binding.rvMyPost.scrollToPosition(position)
                }
                hideLoading()
            } else {
                Toast.makeText(
                    this,
                    "Đã có lỗi xảy ra",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.ivBackArrow.setOnClickListener {
            finish()
        }
    }

    fun showLoading() {
        binding.loadingOverlay.visibility = View.VISIBLE
    }

    fun hideLoading() {
        lifecycleScope.launch(Dispatchers.Main) {
            delay(500)
            binding.loadingOverlay.visibility = View.GONE
        }
    }
}