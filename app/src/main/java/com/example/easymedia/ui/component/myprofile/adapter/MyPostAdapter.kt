package com.example.easymedia.ui.component.myprofile.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.easymedia.data.model.Post
import com.example.easymedia.data.model.User
import com.example.easymedia.databinding.ItemMyPostBinding

class MyPostAdapter(
    private val posts: MutableList<Post>,
    private val switchScreen: (User, Int) -> Unit
) :
    RecyclerView.Adapter<MyPostAdapter.PostViewHolder>() {
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
                switchScreen(user!!, position)
            }
        }
    }

    fun updateUser(user: User?) {
        this.user = user
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateListPost(posts: List<Post>) {
        this.posts.clear()
        this.posts.addAll(posts)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = posts.size
}
