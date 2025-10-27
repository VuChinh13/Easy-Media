package com.example.easymedia.ui.component.myprofile.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.easymedia.data.model.Post
import com.example.easymedia.databinding.ItemMyPostBinding


class MyPostAdapter(
    private val context: Context,
    private val posts: List<Post>
) :
    RecyclerView.Adapter<MyPostAdapter.PostViewHolder>() {

    class PostViewHolder(binding: ItemMyPostBinding) : RecyclerView.ViewHolder(binding.root) {
       val imageView = binding.imageView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemMyPostBinding.inflate(
            LayoutInflater.from(parent.context),parent,false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        Glide.with(holder.imageView.context).load(posts[position].imageUrls[0]).into(holder.imageView)
//        holder.apply {
//            imageView.setOnClickListener {
//                val intent = Intent(context, PostDetailActivity::class.java)
//                context.startActivity(intent)
//            }
//        }
    }


    override fun getItemCount(): Int = posts.size
}
