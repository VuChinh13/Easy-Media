package com.example.easymedia.ui.component.postdetail

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.easymedia.data.model.Location
import com.example.easymedia.data.model.Post
import com.example.easymedia.data.model.User
import com.example.easymedia.databinding.ActivityPostDetailBinding
import com.example.easymedia.ui.component.mapdetail.MapDetailActivity
import com.example.easymedia.ui.component.postdetail.adapter.PostDetailAdapter
import com.example.easymedia.ui.component.updatepost.UpdatePostActivity
import com.example.easymedia.ui.utils.IntentExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Class này sẽ nhận 1 tham số là index -> vị trí click bên Fragment khác
// hiển thị đúng với vị trí index đó
@Suppress("DEPRECATION")
class PostDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPostDetailBinding
    private val myProfileViewModel: PostDetailViewModel by viewModels()
    private var isChanged = false
    private var isChangedEditPost = false
    private var user: User? = null
    private lateinit var postDetailAdapter: PostDetailAdapter
    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val isSucceeds = data?.getBooleanExtra(IntentExtras.RESULT_DATA, false)
            // xử lí khi mà thành công
            if (isSucceeds == true) {
                isChangedEditPost = true
                myProfileViewModel.getUserPosts(user!!.id)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showLoading()
        // Nhận dữ liệu
        user = intent.getParcelableExtra<User>(IntentExtras.EXTRA_USER)
        val position = intent.getIntExtra(IntentExtras.EXTRA_POSITION, 0)
        postDetailAdapter =
            PostDetailAdapter(
                this,
                mutableListOf(),
                user,
                lifecycleScope,
                { location, post ->
                    swithScreenMapDetail(location,post)
                },
                { change() },
                { showLoading() },
                { hideLoading2() },
                { user, post -> switchScreenUpdatePost(user, post) })
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
            returnScreen()
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    returnScreen()

                    // Ngăn không cho callback gọi lại chính nó
                    isEnabled = false

                    // Cho hệ thống thực hiện hành vi Back mặc định
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        )
    }

    fun showLoading() {
        binding.loadingOverlay.visibility = View.VISIBLE
    }

    fun change() {
        this.isChanged = true
    }

    fun returnScreen() {
        val check = isChanged || isChangedEditPost
        val resultIntent = Intent().apply {
            putExtra(IntentExtras.RESULT_DATA, check)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    fun swithScreenMapDetail(location: Location, post: Post) {
        val intent =
            Intent(this, MapDetailActivity::class.java)
        intent.putExtra("lat", location.latitude)
        intent.putExtra("lng", location.longitude)
        intent.putExtra("name", location.address)
        intent.putExtra(IntentExtras.EXTRA_USER, post)
        startActivity(intent)
    }

    fun hideLoading() {
        lifecycleScope.launch(Dispatchers.Main) {
            delay(500)
            binding.loadingOverlay.visibility = View.GONE
        }
    }

    fun hideLoading2() {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.loadingOverlay.visibility = View.GONE
        }
    }

    fun switchScreenUpdatePost(user: User, post: Post) {
        // Chuyển sang Activity EditPost
        val intent = Intent(this, UpdatePostActivity::class.java).apply {
            putExtra(IntentExtras.EXTRA_USER, user)
            putExtra(IntentExtras.EXTRA_POST, post)
        }
        launcher.launch(intent)
    }
}