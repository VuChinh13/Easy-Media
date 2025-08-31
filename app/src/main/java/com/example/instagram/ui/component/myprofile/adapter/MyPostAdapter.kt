package com.example.instagram.ui.component.myprofile.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.instagram.R
import com.example.instagram.data.model.Post
import com.example.instagram.data.repository.AuthRepository
import com.example.instagram.databinding.ItemMyPostBinding
import com.example.instagram.ui.component.updatepost.UpdatePostActivity
import com.example.instagram.ui.component.utils.IntentExtras
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.core.view.isEmpty


class MyPostAdapter(
    private val context: Context,
    private val posts: MutableList<Post>,
    private val userName: String,
    private val userId: String
) :
    RecyclerView.Adapter<MyPostAdapter.PostViewHolder>() {
    private val authRepository = AuthRepository()
    private val adapterScope = CoroutineScope(Dispatchers.IO)


    class PostViewHolder(binding: ItemMyPostBinding) : RecyclerView.ViewHolder(binding.root) {
        val username: TextView = binding.tvUsername
        val caption: TextView = binding.tvContent
        val viewPager: ViewPager2 = binding.viewPager
        val imageAvatar: ImageView = binding.ivAvatar
        val tvCreateAt: TextView = binding.tvCreateAt
        val tvTotalLike: TextView = binding.tvTotalLike
        val toolbar: Toolbar = binding.tbMenu
        val imageLike: ImageView = binding.ivLike
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemMyPostBinding.inflate(
            LayoutInflater.from(parent.context),parent,false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        var liked = false
        val post = posts[position]

        holder.imageLike.setImageResource(R.drawable.ic_heart)
        post.listLike.forEach { userLike ->
            if (userLike.username == userName) {
                holder.imageLike.setImageResource(R.drawable.ic_heart_red)
                liked = true
            }
        }
        holder.username.text = post.author.username
        holder.caption.text = post.content
        holder.tvCreateAt.text = formatCreatedAt(post.createdAt)
        Glide.with(holder.itemView.context).load(post.author.avatar)
            .error(R.drawable.ic_avatar)
            .into(holder.imageAvatar)
        holder.viewPager.adapter = MyPostImagePagerAdapter(post.images)
        holder.tvTotalLike.text = post.totalLike.toString()

        holder.imageLike.setOnClickListener {

            if (liked) {

                adapterScope.launch {
                    val result = authRepository.likePost(userId, post._id, -1)
                    withContext(Dispatchers.Main) {
                        if (result != null) {
                            liked = !liked
                            holder.imageLike.setImageResource(R.drawable.ic_heart)
                            post.totalLike -= 1
                            holder.tvTotalLike.text = post.totalLike.toString()
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Đã có lỗi xảy ra", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                adapterScope.launch {
                    val result = authRepository.likePost(userId, post._id, 1)
                    withContext(Dispatchers.Main) {
                        if (result != null) {
                            liked = !liked
                            holder.imageLike.setImageResource(R.drawable.ic_heart_red)
                            post.totalLike += 1
                            holder.tvTotalLike.text = post.totalLike.toString()
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Đã có lỗi xảy ra", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }


        if (holder.toolbar.menu.isEmpty()) {
            holder.toolbar.inflateMenu(R.menu.menu_item)
        }

        holder.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                /// xoa bai viet
                R.id.menu_delete_post -> {

                    val sharedPreferences =
                        context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                    val userId = sharedPreferences.getString("_id", "") ?: ""
                    adapterScope.launch {
                        val result =
                            authRepository.deletePost(userId, posts[position]._id)
                        withContext(Dispatchers.Main) {
                            if (result != null) {
                                Toast.makeText(
                                    context,
                                    "Xóa bài viết thành công",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()

                                posts.removeAt(position)
                                notifyItemRemoved(position)
                                notifyItemRangeChanged(position, posts.size)
                            } else Toast.makeText(
                                context,
                                "Đã có lỗi không thể xóa bài viết",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    true
                }


                /// chinh sua bai viet
                R.id.menu_edit_post -> {
                    val intent = Intent(context, UpdatePostActivity::class.java).apply {
                        putStringArrayListExtra(
                            IntentExtras.EXTRA_POST_IMAGE,
                            ArrayList(posts[position].images)
                        )
                        putExtra(IntentExtras.EXTRA_POST_CONTENT, posts[position].content)
                        putExtra(IntentExtras.EXTRA_POST_ID, posts[position]._id)
                    }
                    context.startActivity(intent)
                    true
                }

                else -> false
            }
        }
    }

    private fun formatCreatedAt(dateString: String): String {
        val inputFormatter = DateTimeFormatter.ISO_DATE_TIME
        val outputFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)
        val dateTime = LocalDateTime.parse(dateString, inputFormatter)
        return dateTime.format(outputFormatter)
    }

    override fun getItemCount(): Int = posts.size
}
