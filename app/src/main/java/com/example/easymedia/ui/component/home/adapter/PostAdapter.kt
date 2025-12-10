package com.example.easymedia.ui.component.home.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.easymedia.R
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.data.data_source.firebase.FirebaseAuthService
import com.example.easymedia.data.data_source.firebase.FirebasePostService
import com.example.easymedia.data.model.Post
import com.example.easymedia.data.repository.AuthRepositoryImpl
import com.example.easymedia.data.repository.PostRepositoryImpl
import com.example.easymedia.databinding.ItemFirstPostBinding
import com.example.easymedia.databinding.ItemPostBinding
import com.example.easymedia.ui.component.comment.CommentBottomSheet
import com.example.easymedia.ui.component.home.OnAvatarClickListener
import com.example.easymedia.ui.component.utils.SharedPrefer
import com.example.easymedia.ui.component.utils.TimeFormatter
import com.example.easymedia.ui.component.like.LikeBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Trong adapter có hoạt động like, xem comment , hiển thị số like và số comment
// like : cần postID
// ..........
// Làm thêm sự kiện là khi mà nhấn vào ảnh thì xung quanh bị mở đi và có thể
// chuyển qua lại các màn này đi xem
class PostAdapter(
    var posts: MutableList<Post>,
    private val lifecycleCoroutineScope: LifecycleCoroutineScope,
    private val listener: OnAvatarClickListener,
    private val storyAdapter: StoryAdapter
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val postRepository =
        PostRepositoryImpl(FirebasePostService(cloudinary = CloudinaryServiceImpl()))
    private var context: Context? = null
    private val authRepository =
        AuthRepositoryImpl(FirebaseAuthService(CloudinaryServiceImpl()))
    private val userId = SharedPrefer.getId()

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_NORMAL = 1
    }

    class FirstPostViewHolder(binding: ItemFirstPostBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var rcvStory: RecyclerView = binding.rcvStory
    }

    class PostViewHolder(binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root) {
        val username: TextView = binding.tvUsername
        val caption: TextView = binding.tvContent
        val viewPager: ViewPager2 = binding.viewPager
        val imageAvatar: ImageView = binding.ivAvatar
        val tvCreateAt: TextView = binding.tvCreateAt
        val tvTotalLike: TextView = binding.tvTotalLike
        val imageLike: ImageView = binding.ivLike
        val dotsIndicator = binding.dotsIndicator
        val shimmerAvatar = binding.shimmerAvatar
        val shimmerUser = binding.shimmerUsername
        val cardView = binding.cardView
        val btnComment = binding.btnComment
        val tvTotalComment = binding.tvTotalComment
        val tvLocation = binding.tvLocation
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
        if (holder is FirstPostViewHolder) {
            holder.rcvStory.setHasFixedSize(true)
            holder.rcvStory.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            holder.rcvStory.adapter = storyAdapter

        } else if (holder is PostViewHolder) {
            val post = posts[position - 1]
            // 1) Luôn set adapter & attach indicator NGAY, trước khi gọi API
            with(holder) {
                showShimmner(holder)
                viewPager.adapter = ImagePagerAdapter(post.imageUrls)
                dotsIndicator.attachTo(viewPager)

                // Ẩn chấm nếu chỉ có 1 ảnh (nhiều thư viện tự ẩn ta chủ động luôn)
                dotsIndicator.visibility =
                    if (post.imageUrls.size > 1) View.VISIBLE else View.GONE
                caption.text = post.caption
                tvTotalComment.text = post.counts.comments.toString()
                tvCreateAt.text = TimeFormatter.getRelativeTime(post.createdAt)
                tvTotalLike.text = post.counts.likes.toString()

                if (post.location != null) {
                    tvLocation.visibility = View.VISIBLE
                    tvLocation.text = post.location!!.address
                } else {
                    tvLocation.visibility = View.INVISIBLE
                    tvLocation.text = ""
                }
            }

            // Hiển thị những thông tin cần thiết lên
            lifecycleCoroutineScope.launch(Dispatchers.IO) {
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
                            // sau khi mà load xong
                            hideShimmer(holder)
                            // Khi mà thành
                            imageAvatar.setOnClickListener {
                                listener.onAvatarClick(user)
                            }
                            username.setOnClickListener {
                                listener.onAvatarClick(user)
                            }
                        }
                    }
                }.onFailure {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Đã có lỗi xảy ra", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // hiển thị tym đỏ
            lifecycleCoroutineScope.launch(Dispatchers.IO) {
                val result = postRepository.hasUserLiked(post.id, userId)
                result.onSuccess {
                    if (it) {
                        withContext(Dispatchers.Main) {
                            holder.imageLike.setImageResource(R.drawable.ic_heart_red)
                            likeEvent(holder, true, post)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            holder.imageLike.setImageResource(R.drawable.ic_heart)
                            likeEvent(holder, false, post)
                        }
                    }
                }.onFailure {
                    withContext(Dispatchers.Main) {
                        // Làm gì đó
                    }
                }
            }

            // Sự kiện Comment
            holder.btnComment.setOnClickListener {
                val commentSheet =
                    CommentBottomSheet(
                        post.id,
                        userId,
                        position,
                        post.counts.comments,
                        { pos, newTotalComment ->
                            reloadTotalComment(pos, newTotalComment)
                        }, listener
                    )
                commentSheet.show(
                    (holder.itemView.context as AppCompatActivity).supportFragmentManager,
                    "CommentBottomSheet"
                )
            }

            // Sự kiện Comment
            holder.tvTotalComment.setOnClickListener {
                val commentSheet =
                    CommentBottomSheet(
                        post.id,
                        userId,
                        position,
                        post.counts.comments,
                        { pos, newTotalComment ->
                            reloadTotalComment(pos, newTotalComment)
                        }, listener
                    )
                commentSheet.show(
                    (holder.itemView.context as AppCompatActivity).supportFragmentManager,
                    "CommentBottomSheet"
                )
            }


            // sự kiện hiển thị những người mà đã like
            holder.tvTotalLike.setOnClickListener {
                // thực hiện ở đây
                val likeSheet =
                    LikeBottomSheet(
                        post.id, listener
                    )
                likeSheet.show(
                    (holder.itemView.context as AppCompatActivity).supportFragmentManager,
                    "LikeBottomSheet"
                )
            }

            // sự kiện xem bản đồ
            holder.tvLocation.setOnClickListener {
                Log.d("TestLocation", post.location.toString())
                listener.swithScreenMapDetail(post.location!!, post)
            }
        }
    }

    private fun likeEvent(holder: PostViewHolder, isInitiallyLiked: Boolean, post: Post) {
        var liked = isInitiallyLiked

        holder.imageLike.setOnClickListener {
            liked = !liked
            holder.imageLike.setImageResource(
                if (liked) R.drawable.ic_heart_red else R.drawable.ic_heart
            )

            // Cập nhật số lượng like ngay lập tức
            post.counts.likes += if (liked) 1 else -1
            holder.tvTotalLike.text = post.counts.likes.toString()

            // Gọi API theo trạng thái mới
            lifecycleCoroutineScope.launch(Dispatchers.IO) {
                val result = if (liked) {
                    postRepository.likePost(post.id, userId)
                } else {
                    postRepository.unlikePost(post.id, userId)
                }

                result.onFailure {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Đã có lỗi xảy ra", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newItems: List<Post>) {
        posts.clear()
        posts.addAll(newItems)
        notifyDataSetChanged() // Gọi ở đây
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addPosts(newPosts: List<Post>) {
        val startPos = posts.size
        val updatedList = posts.toMutableList()
        updatedList.addAll(newPosts)
        posts = updatedList

        // Chỉ thông báo phần mới
        notifyItemRangeInserted(startPos + 1, newPosts.size)
    }

    override fun getItemCount(): Int {
        return posts.size + 1
    }

    fun reloadTotalComment(position: Int, totalComment: Int) {
        if (position > 0 && position <= posts.size) {
            posts[position - 1].counts.comments = totalComment
            notifyItemChanged(position) // chỉ reload item đó thôi
        }
    }

    fun showShimmner(holder: PostViewHolder) {
        with(holder) {
            shimmerAvatar.visibility = View.VISIBLE
            shimmerUser.visibility = View.VISIBLE
            username.visibility = View.INVISIBLE
            cardView.visibility = View.INVISIBLE
            shimmerAvatar.startShimmer()
            shimmerUser.startShimmer()
        }
    }

    fun hideShimmer(holder: PostViewHolder) {
        with(holder) {
            shimmerAvatar.stopShimmer()
            shimmerAvatar.visibility = View.GONE
            shimmerUser.stopShimmer()
            shimmerUser.visibility = View.GONE
            username.visibility = View.VISIBLE
            cardView.visibility = View.VISIBLE
        }
    }
}

fun Int.dp(context: Context): Int =
    (this * context.resources.displayMetrics.density).toInt()

