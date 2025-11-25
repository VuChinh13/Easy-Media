package com.example.easymedia.ui.component.like.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.easymedia.R
import com.example.easymedia.data.model.User
import com.example.easymedia.databinding.ItemLikeBottomSheetBinding
import com.example.easymedia.ui.component.home.OnAvatarClickListener
import java.text.Normalizer

class LikeAdapter(
    private val originalListUser: MutableList<User>,
    private val onDismissCallback: () -> Unit,
    private val listener: OnAvatarClickListener?
) : RecyclerView.Adapter<LikeAdapter.LikeViewHolder>() {

    // Danh sách này sẽ được dùng để hiển thị lên màn hình
    private var filteredList: MutableList<User> = originalListUser.toMutableList()

    // Bỏ biến selectedPosition nếu không dùng để highlight item (trong code cũ bạn có khai báo nhưng không dùng logic highlight)
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
        // SỬA LỖI: Lấy data từ filteredList thay vì originalListUser
        val user = filteredList[position]

        with(holder) {
            Glide.with(itemView).load(user.profilePicture).error(R.drawable.ic_avatar)
                .into(imageAvatar)
            fullName.text = user.fullName
            username.text = user.username
            itemView.setOnClickListener {
                listener?.onAvatarClick(user)
                onDismissCallback.invoke()
            }
        }
    }

    /** 🔍 Lọc theo tên, bỏ dấu tiếng Việt */
    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        val normalizedQuery = removeVietnameseAccents(query).lowercase()

        filteredList = if (normalizedQuery.isEmpty()) {
            // Nếu query rỗng, hiển thị lại toàn bộ danh sách gốc
            originalListUser.toMutableList()
        } else {
            // Lọc từ danh sách gốc
            originalListUser.filter { user ->
                val titleNormalized = removeVietnameseAccents(user.fullName ?: "").lowercase()
                titleNormalized.contains(normalizedQuery)
            }.toMutableList()
        }

        selectedPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged() // Cập nhật giao diện
    }

    private fun removeVietnameseAccents(str: String): String {
        var text = Normalizer.normalize(str, Normalizer.Form.NFD)
        text = text.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return text.replace("đ", "d").replace("Đ", "D")
    }

    @SuppressLint("NotifyDataSetChanged")
    fun update(newList: List<User>) {
        // Cập nhật lại danh sách gốc
        originalListUser.clear()
        originalListUser.addAll(newList)

        // Reset lại danh sách hiển thị cho bằng danh sách gốc
        filteredList = originalListUser.toMutableList()

        selectedPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    // SỬA LỖI: Trả về size của filteredList
    override fun getItemCount(): Int = filteredList.size
}