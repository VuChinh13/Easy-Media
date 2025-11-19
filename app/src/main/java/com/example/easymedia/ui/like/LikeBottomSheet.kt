package com.example.easymedia.ui.like

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.easymedia.R
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.data.data_source.firebase.FirebasePostService
import com.example.easymedia.data.repository.PostRepositoryImpl
import com.example.easymedia.databinding.LikeBottomSheetBinding
import com.example.easymedia.ui.component.home.OnAvatarClickListener
import com.example.easymedia.ui.like.adapter.LikeAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LikeBottomSheet(val idPost: String, val listener: OnAvatarClickListener?) :
    BottomSheetDialogFragment() {
    private val repositoryPost =
        PostRepositoryImpl(
            FirebasePostService(cloudinary = CloudinaryServiceImpl())
        )

    private lateinit var adapter: LikeAdapter
    private lateinit var binding: LikeBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = LikeBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog as? BottomSheetDialog
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                val displayMetrics = resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels
                val desiredHeight = (screenHeight * 0.75).toInt()

                val layoutParams = sheet.layoutParams
                layoutParams.height = desiredHeight
                sheet.layoutParams = layoutParams

                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.maxHeight = desiredHeight
            }

        val searchView = binding.btnSearchLike
        val searchEditText = searchView.findViewById<AutoCompleteTextView>(
            androidx.appcompat.R.id.search_src_text
        )
        searchEditText.setTextColor(Color.BLACK)
        searchEditText.textSize = 17f
        val searchIcon = searchView.findViewById<ImageView>(
            androidx.appcompat.R.id.search_mag_icon
        )
        searchIcon.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)  // đổi màu icon
        val closeButton = searchView.findViewById<ImageView>(
            androidx.appcompat.R.id.search_close_btn
        )
        closeButton.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)  // đổi màu
        searchEditText.setHintTextColor(Color.LTGRAY)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LikeAdapter(mutableListOf(), { dismiss() }, listener)
        binding.rcvMusic.layoutManager = LinearLayoutManager(context)
        binding.rcvMusic.adapter = adapter

        CoroutineScope(Dispatchers.IO).launch {
            val result = repositoryPost.getUsersWhoLiked(postId = idPost)
            withContext(Dispatchers.Main) {
                if (result.isNotEmpty()) {
                    // nếu mà ko rỗng
                    adapter.update(result)
                } else {
                    binding.tvTitle3.visibility = View.VISIBLE
                    binding.tvTitle4.visibility = View.VISIBLE
                }
            }
        }

        setupSearchView()
    }

    private fun setupSearchView() {
        val searchView = binding.btnSearchLike

        // Lắng nghe sự kiện gõ chữ trong SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                adapter.filter(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText.orEmpty())
                return true
            }
        })
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }
}
