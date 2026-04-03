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
import com.example.easymedia.utils.SharedPrefer
import com.example.easymedia.utils.TimeFormatter
import com.example.easymedia.ui.component.like.LikeBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            with(holder) {
                showShimmner(holder)
                viewPager.adapter = ImagePagerAdapter(post.imageUrls)
                dotsIndicator.attachTo(viewPager)

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
                            hideShimmer(holder)
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
                    }
                }
            }

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

            holder.tvTotalLike.setOnClickListener {
                val likeSheet =
                    LikeBottomSheet(
                        post.id, listener
                    )
                likeSheet.show(
                    (holder.itemView.context as AppCompatActivity).supportFragmentManager,
                    "LikeBottomSheet"
                )
            }

            holder.tvLocation.setOnClickListener {
                listener.switchScreenMapDetail(post.location!!, post)
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

            post.counts.likes += if (liked) 1 else -1
            holder.tvTotalLike.text = post.counts.likes.toString()

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
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addPosts(newPosts: List<Post>) {
        val startPos = posts.size
        val updatedList = posts.toMutableList()
        updatedList.addAll(newPosts)
        posts = updatedList
        notifyItemRangeInserted(startPos + 1, newPosts.size)
    }

    override fun getItemCount(): Int {
        return posts.size + 1
    }

    fun reloadTotalComment(position: Int, totalComment: Int) {
        if (position > 0 && position <= posts.size) {
            posts[position - 1].counts.comments = totalComment
            notifyItemChanged(position)
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

