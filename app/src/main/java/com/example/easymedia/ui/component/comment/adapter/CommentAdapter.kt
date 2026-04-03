package com.example.easymedia.ui.component.comment.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.easymedia.R
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.data.data_source.firebase.FirebaseAuthService
import com.example.easymedia.data.data_source.firebase.FirebasePostService
import com.example.easymedia.data.model.Comment
import com.example.easymedia.data.repository.AuthRepositoryImpl
import com.example.easymedia.data.repository.PostRepositoryImpl
import com.example.easymedia.databinding.ItemCommentBinding
import com.example.easymedia.ui.component.home.OnAvatarClickListener
import com.example.easymedia.utils.SharedPrefer
import com.example.easymedia.utils.TimeFormatter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentAdapter(
    private val listComment: MutableList<Comment>,
    private val postId: String,
    private val context: Context,
    private val onCommentDeleted: () -> Unit,
    private val onDismissCallback: () -> Unit,
    private val listener: OnAvatarClickListener?
) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {
    private val repositoryAuth = AuthRepositoryImpl(FirebaseAuthService(CloudinaryServiceImpl()))
    private val userId = SharedPrefer.getId()
    private val repositoryPost =
        PostRepositoryImpl(FirebasePostService(cloudinary = CloudinaryServiceImpl()))

    class CommentViewHolder(val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {
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
        val comment = listComment[position]
        with(holder) {
            handleInitView(holder)
            CoroutineScope(Dispatchers.IO).launch {
                val result = repositoryAuth.getUserById(comment.userId)
                result.onSuccess { user ->
                    if (user != null)
                        withContext(Dispatchers.Main) {
                            tvUsername.text = user.username
                            tvComment.text = comment.content
                            tvTime.text = TimeFormatter.getRelativeTime(comment.createdAt)
                            Glide.with(itemView.context)
                                .load(user.profilePicture)
                                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                                .error(R.drawable.ic_avatar)
                                .into(imageAvatar)
                            handleDisplayed(holder)
                            itemView.setOnClickListener {
                                listener?.onAvatarClick(user)
                                onDismissCallback.invoke()
                            }
                        }
                }
            }
            if (comment.userId == userId) {
                deleteComment(holder, comment)
            }
        }
    }

    fun deleteComment(holder: CommentViewHolder, comment: Comment) {
        holder.binding.root.setOnLongClickListener {
            Snackbar.make(
                holder.binding.root,
                context.getString(R.string.confirm_delete_comment),
                Snackbar.LENGTH_SHORT
            )
                .setAction(context.getString(R.string.action_delete)) {
                    onDeleteClick(comment) // Gọi callback xóa luôn
                }
                .show()
            true
        }

    }

    fun onDeleteClick(comment: Comment) {
        CoroutineScope(Dispatchers.IO).launch {
            repositoryPost.deleteComment(postId, comment.id)
            withContext(Dispatchers.Main) {
                onCommentDeleted() // gọi callback để Fragment tự reload lại
                // khi mà gọi thì mới cập nhật số comment
            }
        }
    }

    private fun handleInitView(holder: CommentViewHolder) {
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
        }
    }

    private fun handleDisplayed(holder: CommentViewHolder) {
        with(holder) {
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

    override fun getItemCount(): Int = listComment.size
}
