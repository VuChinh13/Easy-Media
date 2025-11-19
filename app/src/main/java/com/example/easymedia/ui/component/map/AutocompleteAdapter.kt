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

        // Hiển thị gợi ý kết hợp cả 3 loại segment
        val displayText = result.segments.joinToString(" ") { segment ->
            when (segment) {
                is AutocompleteSegmentPlainText -> segment.plainText
                is AutocompleteSegmentPoiCategory ->
                    segment.matchedAlternativeName.ifEmpty { segment.poiCategory.name }
                is AutocompleteSegmentBrand -> segment.brand.name
                else -> ""
            }
        }.ifEmpty { "Unknown" }

        holder.tvText.text = displayText
        holder.itemView.setOnClickListener { onClick(result) }
    }
}
