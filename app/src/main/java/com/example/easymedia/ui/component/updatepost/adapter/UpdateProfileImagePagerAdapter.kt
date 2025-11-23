package com.example.easymedia.ui.component.updatepost.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.easymedia.databinding.ItemImageEditPostBinding

class UpdateProfileImagePagerAdapter(
    private val images: MutableList<String>,      // đổi thành MutableList
    private val onDeleteClick: ((Int) -> Unit)? = null,
    private val changeList: (String) -> Unit,
) : RecyclerView.Adapter<UpdateProfileImagePagerAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: ItemImageEditPostBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageEditPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val binding = holder.binding
        if (images.size > 1) {
            binding.btnDeleteImage.visibility = View.VISIBLE
        }
        Glide.with(binding.imageView.context)
            .load(images[position])
            .into(binding.imageView)

        binding.btnDeleteImage.setOnClickListener {
            changeList(images[position])
            removeImage(position)
        }
    }

    override fun getItemCount(): Int = images.size

    // Hàm xóa ảnh
    private fun removeImage(position: Int) {
        if (position in images.indices) {
            images.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, images.size)

            onDeleteClick?.invoke(position) // callback nếu cần biết index bị xóa
        }
    }
}
