package com.example.easymedia.ui.component.music.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.easymedia.data.model.Music
import com.example.easymedia.databinding.ItemMusicBinding
import java.text.Normalizer
import androidx.core.graphics.toColorInt

class MusicAdapter(
    private val originalList: MutableList<Music>,
    private val onItemClick: (Music) -> Unit,
) : RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    private var filteredList: MutableList<Music> = originalList.toMutableList()
    private var selectedPosition: Int = RecyclerView.NO_POSITION  // 👉 vị trí được chọn

    class MusicViewHolder(val binding: ItemMusicBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val binding = ItemMusicBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MusicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val music = filteredList[position]
        val context = holder.itemView.context

        holder.binding.tvTitle.text = music.title
        holder.binding.tvArtist.text = music.artist
        holder.binding.tvDuration.text = music.duration

        // 👇 Đổi màu nền nếu là item được chọn
        if (position == selectedPosition) {
            holder.itemView.setBackgroundColor("#292E34".toColorInt())
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        holder.itemView.setOnClickListener {
            // Cập nhật vị trí được chọn
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition

            // Gọi callback phát nhạc
            onItemClick.invoke(music)

            // Cập nhật lại item cũ và mới để refresh màu nền
            if (previousPosition != RecyclerView.NO_POSITION)
                notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
        }
    }

    override fun getItemCount(): Int = filteredList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: MutableList<Music>) {
        originalList.clear()
        originalList.addAll(newList)
        filteredList = originalList.toMutableList()
        selectedPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    /** 🔍 Lọc theo title bài hát, bỏ dấu tiếng Việt */
    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        val normalizedQuery = removeVietnameseAccents(query).lowercase()

        filteredList = if (normalizedQuery.isEmpty()) {
            originalList.toMutableList()
        } else {
            originalList.filter { music ->
                val titleNormalized = removeVietnameseAccents(music.title).lowercase()
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
}
