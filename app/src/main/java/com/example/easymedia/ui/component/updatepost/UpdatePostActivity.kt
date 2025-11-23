package com.example.easymedia.ui.component.updatepost

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.easymedia.R
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.data.data_source.firebase.FirebasePostService
import com.example.easymedia.data.model.Post
import com.example.easymedia.data.model.User
import com.example.easymedia.data.repository.PostRepositoryImpl
import com.example.easymedia.databinding.ActivityUpdatePostBinding
import com.example.easymedia.ui.component.updatepost.adapter.UpdateProfileImagePagerAdapter
import com.example.easymedia.ui.component.utils.IntentExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class UpdatePostActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUpdatePostBinding
    private val postImages: MutableList<File> = mutableListOf()
    private val listChanged: MutableList<String> = mutableListOf()
    private lateinit var adapter: UpdateProfileImagePagerAdapter
    private val postRepository =
        PostRepositoryImpl(FirebasePostService(cloudinary = CloudinaryServiceImpl()))
    private var user: User? = null
    private var post: Post? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Nhận dữ liệu từ bên màn hình kia truyền sang bên
        user = intent.getParcelableExtra<User>(IntentExtras.EXTRA_USER)
        post = intent.getParcelableExtra<Post>(IntentExtras.EXTRA_POST)

        if (user != null && post != null) {
            adapter = UpdateProfileImagePagerAdapter(
                post?.imageUrls?.toMutableList() ?: mutableListOf()
            ) { url ->
                changeList(url)
            }
            binding.viewPager.adapter = adapter
            binding.tvUsername.text = user!!.username
            binding.tvTotalLike.text = post!!.counts.likes.toString()
            binding.tvTotalComment.text = post!!.counts.comments.toString()
            binding.etContent.setText(post!!.caption)
            Glide.with(this).load(user?.profilePicture)
                .error(R.drawable.ic_avatar)
                .into(binding.ivAvatar)
            binding.dotsIndicator.attachTo(binding.viewPager)

            // Ẩn chấm nếu chỉ có 1 ảnh (nhiều thư viện tự ẩn ta chủ động luôn)
            binding.dotsIndicator.visibility =
                if (post!!.imageUrls.size > 1) View.VISIBLE else View.GONE
        }


        binding.dotsIndicator.attachTo(binding.viewPager)

        // Quan sát khi adapter thay đổi (xóa, thêm ảnh)
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                binding.dotsIndicator.invalidate() // cập nhật lại dots
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                binding.dotsIndicator.invalidate()
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.dotsIndicator.invalidate()
            }
        })

        binding.ivClose.setOnClickListener { finish() }

        // lưu thay đổi
        binding.btSave.setOnClickListener {
            // Kiểm tra xem là có sự thay đổi ko
            if (checkChange(
                    post!!,
                    binding.etContent.text.toString()
                ) || listChanged.isNotEmpty()
            ) {
                binding.loadingOverlay.visibility = View.VISIBLE
                lifecycleScope.launch(Dispatchers.IO) {
                    val result = postRepository.updatePost(
                        post!!,
                        listChanged,
                        binding.etContent.text.toString()
                    )
                    result.onSuccess {
                        withContext(Dispatchers.Main) {
                            binding.loadingOverlay.visibility = View.GONE
                            val resultIntent = Intent().apply {
                                putExtra(IntentExtras.RESULT_DATA, true)
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                    }
                }
            } else {
                finish()
            }
        }
    }

    fun changeList(url: String) {
        listChanged.add(url)
    }

    fun checkChange(post: Post, caption: String): Boolean {
        return post.caption != caption
    }
}
