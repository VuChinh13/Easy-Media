//package com.example.instagram.ui.component.postdetail.adapter
//
//import android.content.Context
//import android.content.Intent
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.widget.Toolbar
//import androidx.recyclerview.widget.RecyclerView
//import androidx.viewpager2.widget.ViewPager2
//import com.bumptech.glide.Glide
//import com.example.instagram.R
//import com.example.instagram.data.model.Post
//import com.example.instagram.data.repository.AuthRepository
//import com.example.instagram.ui.component.updatepost.UpdatePostActivity
//import com.example.instagram.ui.component.utils.IntentExtras
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.time.LocalDateTime
//import java.time.format.DateTimeFormatter
//import java.util.Locale
//
//
//class PostDetailAdapter(
//    private val context: Context,
//    private val posts: MutableList<Post>,
//    private val userName: String,
//    private val userId: String
//) :
//    RecyclerView.Adapter<PostDetailAdapter.PostViewHolder>() {
//    private val authRepository = AuthRepository()
//    private val adapterScope = CoroutineScope(Dispatchers.IO)
//
//    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val username: TextView = view.findViewById(R.id.tv_username)
//        val caption: TextView = view.findViewById(R.id.tv_content)
//        val viewPager: ViewPager2 = view.findViewById(R.id.viewPager)
//        val imageAvatar: ImageView = view.findViewById(R.id.iv_avatar)
//        val tvCreateAt: TextView = view.findViewById(R.id.tv_createAt)
//        val tvTotalLike: TextView = view.findViewById(R.id.tv_total_like)
//        val toolbar: Toolbar = view.findViewById(R.id.tb_menu)
//        val imageLike: ImageView = view.findViewById(R.id.iv_like)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_my_post_detail, parent, false)
//        return PostViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
//        var liked = false
//        val post = posts[position]
//
//        holder.imageLike.setImageResource(R.drawable.ic_heart)
//        post.listLike.forEach { userLike ->
//            if (userLike.username == userName) {
//                holder.imageLike.setImageResource(R.drawable.ic_heart_red)
//                liked = true
//            }
//        }
//        holder.username.text = post.author.username
//        holder.caption.text = post.content
//        holder.tvCreateAt.text = formatCreatedAt(post.createdAt)
//        Glide.with(holder.itemView.context).load(post.author.avatar)
//            .error(R.drawable.ic_avatar)
//            .into(holder.imageAvatar)
//        holder.viewPager.adapter = PostDetailImagePagerAdapter(post.images)
//        holder.tvTotalLike.text = post.totalLike.toString()
//
//        holder.imageLike.setOnClickListener {
//
//            if (liked) {
//
//                adapterScope.launch {
//                    val result = authRepository.likePost(userId, post._id, -1)
//                    withContext(Dispatchers.Main) {
//                        if (result != null) {
//                            liked = !liked
//                            holder.imageLike.setImageResource(R.drawable.ic_heart)
//                            post.totalLike -= 1
//                            holder.tvTotalLike.text = post.totalLike.toString()
//                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
//                        } else {
//                            Toast.makeText(context, "Đã có lỗi xảy ra", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                }
//            } else {
//                adapterScope.launch {
//                    val result = authRepository.likePost(userId, post._id, 1)
//                    withContext(Dispatchers.Main) {
//                        if (result != null) {
//                            liked = !liked
//                            holder.imageLike.setImageResource(R.drawable.ic_heart_red)
//                            post.totalLike += 1
//                            holder.tvTotalLike.text = post.totalLike.toString()
//                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
//                        } else {
//                            Toast.makeText(context, "Đã có lỗi xảy ra", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                }
//            }
//        }
//
//
//        if (holder.toolbar.menu.size() == 0) {
//            holder.toolbar.inflateMenu(R.menu.menu_item)
//        }
//
//        holder.toolbar.setOnMenuItemClickListener { menuItem ->
//            when (menuItem.itemId) {
//                R.id.menu_delete_post -> {
//
//                    val sharedPreferences =
//                        context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
//                    val userId = sharedPreferences.getString("_id", "") ?: ""
//                    adapterScope.launch {
//                        val result =
//                            authRepository.deletePost(userId, posts[position]._id)
//                        withContext(Dispatchers.Main) {
//                            if (result != null) {
//                                Toast.makeText(
//                                    context,
//                                    "Xóa bài viết thành công",
//                                    Toast.LENGTH_SHORT
//                                )
//                                    .show()
//
//                                posts.removeAt(position)
//                                notifyItemRemoved(position)
//                                notifyItemRangeChanged(position, posts.size)
//                            } else Toast.makeText(
//                                context,
//                                "Đã có lỗi không thể xóa bài viết",
//                                Toast.LENGTH_SHORT
//                            ).show()
//                        }
//                    }
//                    true
//                }
//
//                R.id.menu_edit_post -> {
//
//                    val intent = Intent(context, UpdatePostActivity::class.java).apply {
//                        putStringArrayListExtra(
//                            IntentExtras.EXTRA_POST_IMAGE,
//                            ArrayList(posts[position].images)
//                        )
//                        putExtra(IntentExtras.EXTRA_POST_CONTENT, posts[position].content)
//                        putExtra(IntentExtras.EXTRA_POST_ID, posts[position]._id)
//                    }
//                    context.startActivity(intent)
//                    true
//                }
//
//                else -> false
//            }
//        }
//    }
//
//    private fun formatCreatedAt(dateString: String): String {
//        val inputFormatter = DateTimeFormatter.ISO_DATE_TIME
//        val outputFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)
//        val dateTime = LocalDateTime.parse(dateString, inputFormatter)
//        return dateTime.format(outputFormatter)
//    }
//
//    override fun getItemCount(): Int = posts.size
//}