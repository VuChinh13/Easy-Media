//package com.example.instagram.ui.component.profile.adapter
//
//import android.content.Context
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.TextView
//import android.widget.Toast
//import androidx.recyclerview.widget.RecyclerView
//import androidx.viewpager2.widget.ViewPager2
//import com.bumptech.glide.Glide
//import com.example.instagram.R
//import com.example.instagram.data.model.Post
//import com.example.instagram.data.repository.AuthRepository
//import com.example.instagram.databinding.ItemPostBinding
//import com.example.instagram.ui.component.home.adapter.ImagePagerAdapter
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.time.LocalDateTime
//import java.time.format.DateTimeFormatter
//import java.util.Locale
//
//class ProfileAdapter(
//    private val context: Context,
//    private val posts: List<Post>,
//    private val userName: String,
//    private val userId: String
//) :
//    RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder>() {
//    private val authRepository = AuthRepository()
//    private val adapterScope = CoroutineScope(Dispatchers.IO)
//
//    class ProfileViewHolder(binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root) {
//        val username: TextView = binding.tvUsername
//        val caption: TextView = binding.tvContent
//        val viewPager: ViewPager2 = binding.viewPager
//        val imageAvatar: ImageView = binding.ivAvatar
//        val tvCreateAt: TextView = binding.tvCreateAt
//        val tvTotalLike: TextView = binding.tvTotalLike
//        val imageLike: ImageView = binding.ivLike
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
//        val binding = ItemPostBinding.inflate(LayoutInflater.from(context),parent,false)
//        return ProfileViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
//        var liked = false
//        val post = posts[position]
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
//        holder.viewPager.adapter = ImagePagerAdapter(post.images)
//        holder.tvTotalLike.text = post.totalLike.toString()
//
//        holder.imageLike.setOnClickListener {
//            if (liked) {
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
//                            liked = !liked // Cập nhật lại trạng thái đã like hay chưa
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
