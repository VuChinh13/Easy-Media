package com.example.easymedia.ui.component.following

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
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.data.data_source.firebase.FirebaseAuthService
import com.example.easymedia.data.model.User
import com.example.easymedia.data.repository.AuthRepositoryImpl
import com.example.easymedia.databinding.FollowingBottomSheetBinding
import com.example.easymedia.ui.component.following.adpter.FollowingAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.*

class FollowingBottomSheet(
    private val listId: List<String>,
    private val switchScreen: (User) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: FollowingBottomSheetBinding
    private val repositoryAuth = AuthRepositoryImpl(FirebaseAuthService(CloudinaryServiceImpl()))
    private val listUser = mutableListOf<User>()
    private lateinit var adapter: FollowingAdapter

    private val fetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FollowingBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        // Thiết lập chiều cao của BottomSheet
        val dialog = dialog as? BottomSheetDialog
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                val displayMetrics = resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels
                val desiredHeight = (screenHeight * 0.75).toInt()

                sheet.layoutParams.height = desiredHeight
                sheet.requestLayout()

                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.maxHeight = desiredHeight
            }

        // Tùy chỉnh SearchView
        val searchView = binding.btnSearchLike
        val searchEditText =
            searchView.findViewById<AutoCompleteTextView>(androidx.appcompat.R.id.search_src_text)
        searchEditText.setTextColor(Color.BLACK)
        searchEditText.textSize = 17f
        searchEditText.setHintTextColor(Color.LTGRAY)

        val searchIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchIcon.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)

        val closeButton =
            searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeButton.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FollowingAdapter(mutableListOf(), { dismiss() }, { user -> switchScreen(user) })
        binding.rcvMusic.layoutManager = LinearLayoutManager(context)
        binding.rcvMusic.adapter = adapter

        // Fetch users song song
        fetchUsers()

        // Setup search
        setupSearchView()
    }

    private fun fetchUsers() {
        fetchScope.launch {
            // Gọi getUserById song song
            val deferredList = listId.map { uid ->
                async {
                    repositoryAuth.getUserById(uid).getOrNull()
                }
            }

            val users = deferredList.awaitAll().filterNotNull()

            withContext(Dispatchers.Main) {
                if (users.isNotEmpty()) {
                    listUser.clear()
                    listUser.addAll(users)
                    adapter.update(listUser)
                } else {
                    binding.tvTitle3.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupSearchView() {
        val searchView = binding.btnSearchLike
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
        fetchScope.cancel() // Hủy coroutine nếu bottom sheet bị đóng
    }
}
