// file: AutocompleteAdapter.kt
package com.example.easymedia.ui.component.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.easymedia.R
import com.tomtom.sdk.search.model.result.AutocompleteResult


class AutocompleteAdapter(
    private var items: List<DisplayItem> = emptyList(),
    private val onClick: (DisplayItem) -> Unit
) : RecyclerView.Adapter<AutocompleteAdapter.ViewHolder>() {

    fun updateData(newItems: List<DisplayItem>) {
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
        val item = items[position]

        val displayText = when(item) {
            is DisplayItem.History -> item.item.address
            is DisplayItem.Suggestion -> {
                // Build queryText từ segments
                val segments = item.result.segments.mapNotNull { segment ->
                    when(segment) {
                        is com.tomtom.sdk.search.model.result.AutocompleteSegmentPlainText -> segment.plainText.trim().takeIf { it.isNotEmpty() }
                        is com.tomtom.sdk.search.model.result.AutocompleteSegmentPoiCategory -> (segment.matchedAlternativeName.ifEmpty { segment.poiCategory.name }).trim().takeIf { it.isNotEmpty() }
                        is com.tomtom.sdk.search.model.result.AutocompleteSegmentBrand -> segment.brand.name.trim().takeIf { it.isNotEmpty() }
                        else -> null
                    }
                }
                segments.joinToString(", ").ifEmpty { item.result.toString() }
            }
        }

        holder.tvText.text = displayText
        holder.itemView.setOnClickListener { onClick(item) }
    }
}
