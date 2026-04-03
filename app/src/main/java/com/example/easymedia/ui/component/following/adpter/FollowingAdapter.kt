package com.example.easymedia.ui.component.following.adpter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.easymedia.R
import com.example.easymedia.data.model.User
import com.example.easymedia.databinding.ItemLikeBottomSheetBinding
import com.example.easymedia.utils.SharedPrefer
import java.text.Normalizer

class FollowingAdapter(
    private val originalListUser: MutableList<User>,
    private val onDismissCallback: () -> Unit,
    private val switchScreen: (User) -> Unit
) : RecyclerView.Adapter<FollowingAdapter.LikeViewHolder>() {
    private var filteredList: MutableList<User> = originalListUser.toMutableList()
    private var selectedPosition: Int = RecyclerView.NO_POSITION
    class LikeViewHolder(val binding: ItemLikeBottomSheetBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val imageAvatar = binding.ivUserStory
        val fullName = binding.txtName
        val username = binding.txtUsername
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LikeViewHolder {
        val binding = ItemLikeBottomSheetBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LikeViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: LikeViewHolder,
        position: Int
    ) {
        val user = filteredList[position]
        with(holder) {
            Glide.with(itemView).load(user.profilePicture).error(R.drawable.ic_avatar)
                .into(imageAvatar)
            fullName.text = user.fullName
            username.text = user.username
            if (SharedPrefer.getId() != user.id) {
                itemView.setOnClickListener {
                    switchScreen(user)
                    onDismissCallback.invoke()
                }
            }
        }
    }

    /** Lọc theo tên, bỏ dấu tiếng Việt */
    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        val normalizedQuery = removeVietnameseAccents(query).lowercase()
        filteredList = if (normalizedQuery.isEmpty()) {
            originalListUser.toMutableList()
        } else {
            originalListUser.filter { user ->
                val titleNormalized = removeVietnameseAccents(user.fullName).lowercase()
                titleNormalized.contains(normalizedQuery)
            }.toMutableList()
        }

        selectedPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    private fun removeVietnameseAccents(str: String): String {
        var text = Normalizer.normalize(str, Normalizer.Form.NFD)
        text = text.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return text.replace("đ", "d").replace("Đ", "D")
    }

    @SuppressLint("NotifyDataSetChanged")
    fun update(newList: List<User>) {
        originalListUser.clear()
        originalListUser.addAll(newList)
        filteredList = originalListUser.toMutableList()
        selectedPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = filteredList.size
}