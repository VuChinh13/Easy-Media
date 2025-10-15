package com.example.instagram.ui.component.comment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.instagram.R
import com.example.instagram.data.data_source.firebase.FirebaseAuthService
import com.example.instagram.data.model.Comment
import com.example.instagram.data.repository.AuthRepositoryImpl
import com.example.instagram.databinding.ItemCommentBinding
import com.example.instagram.ui.component.utils.TimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentAdapter(
    private val listComment: List<Comment>
) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {
    private val repositoryAuth = AuthRepositoryImpl(FirebaseAuthService())

    class CommentViewHolder(binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        val imageAvatar = binding.ivAvatar
        val tvComment = binding.tvComment
        val tvUsername = binding.tvUsername
        val tvTime = binding.tvTime
        val cardView = binding.cardView
        val shimmerAvatar = binding.shimmerAvatar
        val shimmerUser = binding.shimmerUsername
        val shimmerComment = binding.shimmerContent
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: CommentViewHolder,
        position: Int
    ) {
        val post = listComment[position]
        // Chỗ này thì cần gọi API tiếp
        with(holder) {
            shimmerAvatar.visibility = View.VISIBLE
            shimmerUser.visibility = View.VISIBLE
            shimmerComment.visibility = View.VISIBLE

            tvUsername.visibility = View.INVISIBLE
            tvTime.visibility = View.INVISIBLE
            cardView.visibility = View.INVISIBLE
            tvComment.visibility = View.INVISIBLE

            shimmerAvatar.startShimmer()
            shimmerUser.startShimmer()
            shimmerComment.startShimmer()

            CoroutineScope(Dispatchers.IO).launch {
                val result = repositoryAuth.getUserById(post.userId)
                result.onSuccess { user ->
                    if (user != null)
                        withContext(Dispatchers.Main) {
                            // Gán nội dung trước
                            tvUsername.text = user.username
                            tvComment.text = post.content
                            tvTime.text = TimeFormatter.getRelativeTime(post.createdAt)

                            // Load ảnh (có thể async riêng)
                            Glide.with(itemView.context)
                                .load(user.profilePicture)
                                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                                .error(R.drawable.ic_avatar)
                                .into(imageAvatar)

                            // Sau khi set xong → tắt shimmer, hiển thị thật
                            shimmerAvatar.stopShimmer()
                            shimmerUser.stopShimmer()
                            shimmerComment.stopShimmer()

                            shimmerAvatar.visibility = View.GONE
                            shimmerUser.visibility = View.GONE
                            shimmerComment.visibility = View.GONE

                            tvUsername.visibility = View.VISIBLE
                            cardView.visibility = View.VISIBLE
                            tvComment.visibility = View.VISIBLE
                            tvTime.visibility = View.VISIBLE
                        }
                }
            }

        }
    }

    override fun getItemCount(): Int = listComment.size
}
