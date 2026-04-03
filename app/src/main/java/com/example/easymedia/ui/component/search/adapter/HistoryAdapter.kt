package com.example.easymedia.ui.component.search.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.easymedia.R
import com.example.easymedia.data.model.User
import com.example.easymedia.databinding.ItemSuggestionBinding
import com.example.easymedia.ui.component.main.MainActivity
import com.example.easymedia.utils.SharedPrefer

// Nhận vào bên trong đó là danh sách lịch sử tìm kiếm
class HistoryAdapter(
    private var historyUsers: MutableList<User>,
    private val clearItemHistory: (userId: String) -> Unit,
    private val listener: (user: User) -> Unit,
    private val activity: MainActivity
) : RecyclerView.Adapter<HistoryAdapter.UserViewHolder>() {
    class UserViewHolder(binding: ItemSuggestionBinding) : RecyclerView.ViewHolder(binding.root) {
        val username: TextView = binding.txtUsername
        val name: TextView = binding.txtName
        val imageProfile = binding.ivUserStory
        val btnClose = binding.btnClose
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding =
            ItemSuggestionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        with(holder) {
            btnClose.visibility = View.VISIBLE
            username.text = historyUsers[position].username
            name.text = historyUsers[position].fullName
            Glide.with(holder.itemView).load(historyUsers[position].profilePicture)
                .error(R.drawable.ic_avatar)
                .into(imageProfile)

            // xóa khỏi adapter đi
            btnClose.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < historyUsers.size) {
                    val removedUser = historyUsers[pos]
                    clearItemHistory(removedUser.id)

                    // Xóa phần tử
                    historyUsers.removeAt(pos)
                    notifyItemRemoved(pos)

                    // Nếu list trống, refresh toàn bộ để RecyclerView reset lại state
                    if (historyUsers.isEmpty()) {
                        notifyDataSetChanged()
                    }
                }
            }

            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val user = historyUsers[pos]
                    if (user.id != SharedPrefer.getId()) {
                        listener(user)
                    } else {
                        activity.switchScreenMyProfile()
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return historyUsers.size
    }

    // 👉 Hàm thêm 1 phần tử mới vào đầu danh sách
    fun addUser(user: User) {
        // Xóa nếu đã tồn tại user đó
        val existingIndex = historyUsers.indexOfFirst { it.id == user.id }
        if (existingIndex != -1) {
            historyUsers.removeAt(existingIndex)
            notifyItemRemoved(existingIndex)
        }

        // Thêm mới vào đầu danh sách
        historyUsers.add(0, user)
        notifyItemInserted(0)
    }
}