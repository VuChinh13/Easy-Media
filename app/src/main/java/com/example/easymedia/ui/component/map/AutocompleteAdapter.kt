package com.example.easymedia.ui.component.map

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.easymedia.R
import com.tomtom.sdk.search.model.result.AutocompleteResult
import com.tomtom.sdk.search.model.result.AutocompleteSegmentBrand
import com.tomtom.sdk.search.model.result.AutocompleteSegmentPlainText
import com.tomtom.sdk.search.model.result.AutocompleteSegmentPoiCategory

class AutocompleteAdapter(
    private var items: List<AutocompleteResult> = emptyList(),
    private val onClick: (AutocompleteResult) -> Unit
) : RecyclerView.Adapter<AutocompleteAdapter.ViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newItems: List<AutocompleteResult>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvText: TextView = view.findViewById(R.id.tv_suggestion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggestion_map, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = items[position]

        val segmentStrings = result.segments.mapNotNull { segment ->
            when (segment) {
                is AutocompleteSegmentPlainText -> segment.plainText.ifEmpty { null }
                is AutocompleteSegmentPoiCategory -> {
                    // Nếu không có tên khớp thay thế, sử dụng tên danh mục POI
                    segment.matchedAlternativeName.ifEmpty { segment.poiCategory.name }
                        .ifEmpty { null }
                }

                is AutocompleteSegmentBrand -> segment.brand.name.ifEmpty { null }
                else -> null // Loại bỏ các loại segment không cần thiết hoặc chuỗi rỗng
            }
        }

        // Chỉ join các chuỗi không rỗng bằng dấu phẩy và dấu cách
        val displayText = segmentStrings.joinToString(", ").ifEmpty { "Không có gợi ý" }
        // --- KẾT THÚC CODE ĐỀ XUẤT CẢI TIẾN ---

        holder.tvText.text = displayText
        holder.itemView.setOnClickListener { onClick(result) }
    }
}
