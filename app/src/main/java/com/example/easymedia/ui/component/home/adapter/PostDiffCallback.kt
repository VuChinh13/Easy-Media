package com.example.easymedia.ui.component.home.adapter

import androidx.recyclerview.widget.DiffUtil
import com.example.easymedia.data.model.Post

class PostDiffCallback(
    private val oldList: List<Post>,
    private val newList: List<Post>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // So sánh ID — nếu cùng ID thì là cùng bài viết
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // So sánh nội dung — nếu giống hết thì không cần cập nhật
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
