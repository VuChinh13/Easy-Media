package com.example.instagram.ui.component.home.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.instagram.R
import com.example.instagram.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.instagram.data.data_source.firebase.FirebaseAuthService
import com.example.instagram.data.data_source.firebase.FirebasePostService
import com.example.instagram.data.model.Post
import com.example.instagram.data.model.User
import com.example.instagram.data.repository.AuthRepositoryImpl
import com.example.instagram.data.repository.PostRepositoryImpl
import com.example.instagram.databinding.ItemFirstPostBinding
import com.example.instagram.databinding.ItemPostBinding
import com.example.instagram.ui.component.utils.SharedPrefer
import com.example.instagram.ui.component.utils.TimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Trong adapter có hoạt động like, xem comment , hiển thị số like và số comment
// like : cần postID
// ..........
class PostAdapter(
    private val posts: List<Post>,
    private val listUser: List<User>, // danh sách người dùng -> để hiển thị như kiểu story
    private val user: User
//    private val listener: OnAvatarClickListener
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val postRepository =
        PostRepositoryImpl(FirebasePostService(cloudinary = CloudinaryServiceImpl()))
    private var context: Context? = null
    private val authRepository =
        AuthRepositoryImpl(FirebaseAuthService())
    private val adapterScope = CoroutineScope(Dispatchers.IO)
    private var check = false

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_NORMAL = 1
    }

    class FirstPostViewHolder(binding: ItemFirstPostBinding) :
        RecyclerView.ViewHolder(binding.root), LikeableViewHolder {
        val imageUserStory: ImageView = binding.ivUserStory
        val story: LinearLayout = binding.story
        val username: TextView = binding.tvUsername
        val caption: TextView = binding.tvContent
        val viewPager: ViewPager2 = binding.viewPager
        val imageAvatar: ImageView = binding.ivAvatar
        val tvCreateAt: TextView = binding.tvCreateAt
        override val tvTotalLike: TextView = binding.tvTotalLike
        override val imageLike: ImageView = binding.ivLike
        val dotsIndicator = binding.dotsIndicator
    }

    class PostViewHolder(binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root),
        LikeableViewHolder {
        val username: TextView = binding.tvUsername
        val caption: TextView = binding.tvContent
        val viewPager: ViewPager2 = binding.viewPager
        val imageAvatar: ImageView = binding.ivAvatar
        val tvCreateAt: TextView = binding.tvCreateAt
        override val tvTotalLike: TextView = binding.tvTotalLike
        override val imageLike: ImageView = binding.ivLike
        val dotsIndicator = binding.dotsIndicator
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        return if (viewType == VIEW_TYPE_NORMAL) {
            val binding =
                ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            PostViewHolder(binding)
        } else {
            val binding =
                ItemFirstPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            FirstPostViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var liked = false
        val post = posts[position]
        if (holder is FirstPostViewHolder && !check) {
            // 1) Luôn set adapter & attach indicator NGAY, trước khi gọi API
            with(holder) {
                viewPager.adapter = ImagePagerAdapter(post.imageUrls)
                dotsIndicator.attachTo(viewPager)

                // Ẩn chấm nếu chỉ có 1 ảnh (nhiều thư viện tự ẩn; ta chủ động luôn)
                dotsIndicator.visibility =
                    if (post.imageUrls.size > 1) View.VISIBLE else View.GONE
                caption.text = post.caption
                tvCreateAt.text = TimeFormatter.getRelativeTime(post.createdAt)
                tvTotalLike.text = post.counts.likes.toString()
            }

            // Hiển thị dữ liệu cho item đầu tiên
            listUser.forEach { user ->
                val storyItemView =
                    LayoutInflater.from(context).inflate(R.layout.story_item, holder.story, false)
                val imageView: ImageView =
                    storyItemView.findViewById(R.id.iv_user_story)

                Glide.with(holder.itemView.context)
                    .load(user.profilePicture)
                    .error(R.drawable.ic_avatar)
                    .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                    .into(imageView)

                val textView: TextView =
                    storyItemView.findViewById(R.id.textView2)
                textView.text = user.username
                holder.story.addView(storyItemView)
            }

            // Hiển thị item story chính chủ (của người dùng)
            Glide.with(holder.itemView.context)
                .load(user.profilePicture)
                .error(R.drawable.ic_avatar)
                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                .into(holder.imageUserStory)

            // Hiển thị tym đỏ ở bài viết
            CoroutineScope(Dispatchers.IO).launch {
                val result = postRepository.hasUserLiked(post.id, SharedPrefer.getId())
                result.onSuccess {
                    if (it) {
                        withContext(Dispatchers.Main) {
                            holder.imageLike.setImageResource(R.drawable.ic_heart_red)
                            liked = true
                        }
                    } else {
                        holder.imageLike.setImageResource(R.drawable.ic_heart)
                    }
                }.onFailure {
                    withContext(Dispatchers.Main) {
                        liked = false
                    }
                }
            }

            // Sự kiện khi mà nhấn tym
            liked = likeEvent(holder, liked, post)

            // Hiển thị những thông tin cần thiết lên UI
            // 2) Phần fetch user làm sau, không ảnh hưởng indicator
            adapterScope.launch {
                val result = authRepository.getUserById(post.userId)
                result.onSuccess { user ->
                    withContext(Dispatchers.Main) {
                        with(holder) {
                            username.text = user?.username
                            Glide.with(itemView.context)
                                .load(user?.profilePicture)
                                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                                .error(R.drawable.ic_avatar)
                                .into(imageAvatar)
                        }
                    }
                }.onFailure {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Đã có lỗi xảy ra", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else if (holder is PostViewHolder) {
            // 1) Luôn set adapter & attach indicator NGAY, trước khi gọi API
            with(holder) {
                viewPager.adapter = ImagePagerAdapter(post.imageUrls)
                dotsIndicator.attachTo(viewPager)

                // Ẩn chấm nếu chỉ có 1 ảnh (nhiều thư viện tự ẩn; ta chủ động luôn)
                dotsIndicator.visibility =
                    if (post.imageUrls.size > 1) View.VISIBLE else View.GONE
                caption.text = post.caption
                tvCreateAt.text = TimeFormatter.getRelativeTime(post.createdAt)
                tvTotalLike.text = post.counts.likes.toString()
            }

            // hiển thị tym đỏ
            CoroutineScope(Dispatchers.IO).launch {
                val result = postRepository.hasUserLiked(post.id, SharedPrefer.getId())
                result.onSuccess {
                    if (it) {
                        withContext(Dispatchers.Main) {
                            holder.imageLike.setImageResource(R.drawable.ic_heart_red)
                            liked = true
                        }
                    } else {
                        holder.imageLike.setImageResource(R.drawable.ic_heart)
                    }
                }.onFailure {
                    withContext(Dispatchers.Main) {
                        liked = false
                    }
                }
            }

            // Sự kiện khi mà nhấn tym
            liked = likeEvent(holder, liked, post)

            // Hiển thị những thông tin cần thiết lên UI
            adapterScope.launch {
                val result = authRepository.getUserById(post.userId)
                result.onSuccess { user ->
                    withContext(Dispatchers.Main) {
                        with(holder) {
                            username.text = user?.username
                            Glide.with(itemView.context)
                                .load(user?.profilePicture)
                                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                                .error(R.drawable.ic_avatar)
                                .into(imageAvatar)
                        }
                    }
                }.onFailure {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Đã có lỗi xảy ra", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun likeEvent(
        holder: LikeableViewHolder,
        liked: Boolean,
        post: Post
    ): Boolean {
        var liked1 = liked
        holder.imageLike.setOnClickListener {
            if (liked1) {
                holder.imageLike.setImageResource(R.drawable.ic_heart)
                liked1 = !liked1
                post.counts.likes -= 1
                holder.tvTotalLike.text = post.counts.likes.toString()

                adapterScope.launch {
                    val result = postRepository.unlikePost(post.id, SharedPrefer.getId())
                    result.onFailure {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Đã có lỗi xảy ra", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                holder.imageLike.setImageResource(R.drawable.ic_heart_red)
                liked1 = !liked1
                post.counts.likes += 1
                holder.tvTotalLike.text = post.counts.likes.toString()

                adapterScope.launch {
                    val result = postRepository.likePost(post.id, SharedPrefer.getId())
                    result.onFailure {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Đã có lỗi xảy ra", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        return liked1
    }

    override fun getItemCount(): Int = posts.size
}

interface OnAvatarClickListener {
    fun onAvatarClick(username: String)
}

interface LikeableViewHolder {
    val imageLike: ImageView
    val tvTotalLike: TextView
}

