package com.example.instagram.ui.component.home.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.instagram.R
import com.facebook.shimmer.ShimmerFrameLayout

class ImagePagerAdapter(private val images: List<String>) :
    RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val shimmerImagePost: ShimmerFrameLayout = view.findViewById(R.id.shimmerImagePost)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        with(holder) {
            Glide.with(holder.imageView.context).load(images[position]).into(holder.imageView)
            shimmerImagePost.stopShimmer()
            shimmerImagePost.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = images.size
}
