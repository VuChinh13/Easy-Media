package com.example.easymedia.ui.component.home.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.easymedia.R
import com.example.easymedia.data.model.User
import com.example.easymedia.databinding.ItemFirstStoryBinding
import com.example.easymedia.databinding.StoryItemBinding
import com.example.easymedia.ui.component.utils.SharedPrefer

class StoryAdapter(val listStory: List<User>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    lateinit var context: Context
    companion object {
        private const val VIEW_TYPE_FIRST = 0
        private const val VIEW_TYPE_NORMAL = 1
    }

    class FirstStoryViewHolder(binding: ItemFirstStoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val ivStory = binding.ivUserStory
    }

    class NormalStoryViewHolder(binding: StoryItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val ivStory = binding.ivUserStory
        val txtUsername = binding.txtUsername
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_FIRST else VIEW_TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        return if (viewType == VIEW_TYPE_FIRST) {
            val binding =
                ItemFirstStoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            FirstStoryViewHolder(binding)
        } else {
            val binding =
                StoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            NormalStoryViewHolder(binding)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        with(holder) {
            if (this is FirstStoryViewHolder) {
                // hiển thị story của mình
                val profilePicture = SharedPrefer.getProfilePicture()
                Glide.with(itemView.context)
                    .load(profilePicture)
                    .error(R.drawable.ic_avatar)
                    .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                    .into(ivStory)
            } else if (this is NormalStoryViewHolder) {
                Glide.with(itemView.context)
                    .load(listStory[position - 1].profilePicture)
                    .error(R.drawable.ic_avatar)
                    .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                    .into(ivStory)
                txtUsername.text = listStory[position - 1].username
            }
        }
    }

    override fun getItemCount(): Int = listStory.size + 1
}