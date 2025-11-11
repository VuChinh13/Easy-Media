package com.example.easymedia.ui.component.postdetail.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.easymedia.R
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.data.data_source.firebase.FirebasePostService
import com.example.easymedia.data.model.Post
import com.example.easymedia.data.model.User
import com.example.easymedia.data.repository.PostRepositoryImpl
import com.example.easymedia.databinding.ItemPostBinding
import com.example.easymedia.ui.component.comment.CommentBottomSheet
import com.example.easymedia.ui.component.home.adapter.ImagePagerAdapter
import com.example.easymedia.ui.component.utils.SharedPrefer
import com.example.easymedia.ui.component.utils.TimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PostDetailAdapter(
    private val context: Context,
    private var posts: MutableList<Post>,
    private val user: User?,
    private val scope: LifecycleCoroutineScope
) : RecyclerView.Adapter<PostDetailAdapter.PostViewHolder>() {
    private val postRepository =
        PostRepositoryImpl(FirebasePostService(cloudinary = CloudinaryServiceImpl()))
    private val userId = SharedPrefer.getId()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding =
            ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
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
        val btnComment = binding.btnComment
        val tvTotalComment = binding.tvTotalComment
        val shimmerAvatar = binding.shimmerAvatar
        val shimmerUser = binding.shimmerUsername
        val cardView = binding.cardView
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        with(holder) {
            showShimmner(this)
            viewPager.adapter = ImagePagerAdapter(post.imageUrls)
            dotsIndicator.attachTo(viewPager)

            // Ẩn chấm nếu chỉ có 1 ảnh (nhiều thư viện tự ẩn ta chủ động luôn)
            dotsIndicator.visibility =
                if (post.imageUrls.size > 1) View.VISIBLE else View.GONE
            caption.text = post.caption
            username.text = user?.username
            tvTotalComment.text = post.counts.comments.toString()
            tvCreateAt.text = TimeFormatter.getRelativeTime(post.createdAt)
            tvTotalLike.text = post.counts.likes.toString()

            Glide.with(itemView.context)
                .load(user?.profilePicture)
                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                .error(R.drawable.ic_avatar)
                .into(imageAvatar)
            hideShimmer(holder)

            // hiển thị tym đỏ
            scope.launch(Dispatchers.IO) {
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
                        post.counts.comments
                    ) { pos, newTotalComment ->
                        reloadTotalComment(pos, newTotalComment)
                    }
                commentSheet.show(
                    (holder.itemView.context as AppCompatActivity).supportFragmentManager,
                    "CommentBottomSheet"
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return posts.size
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
            scope.launch(Dispatchers.IO) {
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

    fun reloadTotalComment(position: Int, totalComment: Int) {
        if (position > 0 && position <= posts.size) {
            posts[position].counts.comments = totalComment
            notifyItemChanged(position)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addPosts(newPosts: List<Post>) {
        posts.addAll(newPosts)
        notifyDataSetChanged()
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
