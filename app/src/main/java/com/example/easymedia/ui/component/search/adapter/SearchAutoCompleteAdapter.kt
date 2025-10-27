//package com.example.instagram.ui.component.search.adapter
//
//import android.content.Context
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ArrayAdapter
//import android.widget.Filter
//import android.widget.Filterable
//import com.bumptech.glide.Glide
//import com.example.instagram.R
//import com.example.instagram.data.model.User
//import com.example.instagram.databinding.ItemSuggestionBinding
//
//// Adapter "dumb": không gọi API, chỉ hiển thị dữ liệu hiện có.
//class SearchAutoCompleteAdapter(context: Context) : ArrayAdapter<User>(context, 0), Filterable {
//
//    private val allUsers = mutableListOf<User>()
//    private val visibleUsers = mutableListOf<User>()
//
//    override fun getCount() = visibleUsers.size
//    override fun getItem(position: Int) = visibleUsers.getOrNull(position)
//
//    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
//        val binding: ItemSuggestionBinding
//        val view: View
//        if (convertView == null) {
//            binding = ItemSuggestionBinding.inflate(LayoutInflater.from(context), parent, false)
//            view = binding.root
//            view.tag = binding
//        } else {
//            binding = convertView.tag as ItemSuggestionBinding
//            view = convertView
//        }
//
//        val item = getItem(position) ?: return view
//        Glide.with(context).load(item.avatar)
//            .error(R.drawable.ic_avatar)
//            .into(binding.ivUserStory)
//        binding.txtName.text = item.name.orEmpty()
//        binding.txtUsername.text = item.username.orEmpty()
//        return view
//    }
//
//    // gọi khi bạn đã có list từ API
//    fun replace(newItems: List<User>) {
//        allUsers.clear()
//        allUsers.addAll(newItems)
//        visibleUsers.clear()
//        visibleUsers.addAll(newItems)
//        notifyDataSetChanged()
//    }
//
//    override fun getFilter(): Filter = object : Filter() {
//        override fun performFiltering(constraint: CharSequence?): FilterResults {
//            val q = constraint?.toString()?.trim()?.lowercase().orEmpty()
//            val filtered = if (q.isEmpty()) {
//                allUsers
//            } else {
//                allUsers.filter {
//                    (it.username?.lowercase()?.contains(q) == true) ||
//                            (it.name?.lowercase()?.contains(q) == true)
//                }
//            }
//            return FilterResults().apply {
//                values = filtered
//                count = filtered.size
//            }
//        }
//
//        @Suppress("UNCHECKED_CAST")
//        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
//            visibleUsers.clear()
//            visibleUsers.addAll(results?.values as? List<User> ?: emptyList())
//            notifyDataSetChanged()
//        }
//
//        override fun convertResultToString(value: Any?): CharSequence {
//            val user = value as? User ?: return ""
//            return user.username ?: user.name.orEmpty()
//        }
//    }
//}
//
