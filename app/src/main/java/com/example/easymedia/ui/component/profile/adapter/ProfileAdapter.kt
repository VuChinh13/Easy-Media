package com.example.easymedia.ui.component.profile.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.easymedia.data.model.Post
import com.example.easymedia.data.model.User
import com.example.easymedia.databinding.ItemMyPostBinding
import com.example.easymedia.ui.component.postdetail.PostDetailActivity
import com.example.easymedia.ui.component.utils.IntentExtras

class ProfileAdapter(
    private val context: Context,
    private val posts: MutableList<Post>
) :
    RecyclerView.Adapter<ProfileAdapter.PostViewHolder>() {
    private var user: User? = null
    class PostViewHolder(binding: ItemMyPostBinding) : RecyclerView.ViewHolder(binding.root) {
        val imageView = binding.imageView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemMyPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        Glide.with(holder.imageView.context).load(posts[position].imageUrls[0])
            .into(holder.imageView)
        holder.apply {
            imageView.setOnClickListener {
                // truyền dữ liệu từ màn này sang bên màn bài viết
                val intent = Intent(context, PostDetailActivity::class.java)
                intent.putExtra(IntentExtras.EXTRA_USER, user)
                intent.putExtra(IntentExtras.EXTRA_POSITION, position)
                context.startActivity(intent)
            }
        }
    }

    fun updateUser(user: User?) {
        this.user = user
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateListPost(posts: List<Post>){
        this.posts.addAll(posts)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = posts.size
}
