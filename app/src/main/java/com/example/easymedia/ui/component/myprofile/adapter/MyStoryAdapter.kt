package com.example.easymedia.ui.component.myprofile.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.easymedia.R
import com.example.easymedia.data.model.Story
import com.example.easymedia.databinding.MyStoryItemBinding

class MyStoryAdapter(
    val listStory: MutableList<Story>,
    val listenerStory: (List<Story>) -> Unit
) :
    RecyclerView.Adapter<MyStoryAdapter.NormalStoryViewHolder>() {
    lateinit var context: Context

    class NormalStoryViewHolder(binding: MyStoryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val ivStory = binding.ivUserStory
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NormalStoryViewHolder {
        context = parent.context
        val binding =
            MyStoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NormalStoryViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: NormalStoryViewHolder,
        position: Int
    ) {
        with(holder) {
            Glide.with(itemView.context)
                .load(listStory[position].thumbnailUrl)
                .error(R.drawable.ic_avatar)
                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                .into(ivStory)
            val listStory = listOf<Story>(listStory[position])
            itemView.setOnClickListener {
                listenerStory(listStory)
            }
        }
    }

    override fun getItemCount(): Int = listStory.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateListStory(newList: List<Story>) {
        listStory.clear()
        listStory.addAll(newList)
        notifyDataSetChanged()
    }
}