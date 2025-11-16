package com.example.easymedia.ui.component.home

import android.app.Activity.RESULT_OK
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.easymedia.R
import com.example.easymedia.data.model.Story
import com.example.easymedia.data.model.User
import com.example.easymedia.databinding.FragmentHomeBinding
import com.example.easymedia.ui.component.animation.FragmentTransactionAnimation.setSlideAnimations
import com.example.easymedia.ui.component.home.adapter.PostAdapter
import com.example.easymedia.ui.component.home.adapter.StoryAdapter
import com.example.easymedia.ui.component.main.MainActivity
import com.example.easymedia.ui.component.profile.ProfileFragment
import com.example.easymedia.ui.component.story.StoryActivity
import com.example.easymedia.ui.component.utils.IntentExtras
import com.example.easymedia.ui.component.utils.SharedPrefer
import com.example.easymedia.ui.component.viewstory.ViewStoryActivity

/**
 * Xử lí việc phân trang
 *  - Hiển thị chỉ hiển thị hết các bài viết như bình thường nếu mà người mình theo dõi thì
 *    hiển thị trước
 */
class HomeFragment : Fragment(), OnAvatarClickListener {
    private lateinit var binding: FragmentHomeBinding
    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var postAdapter: PostAdapter
    private lateinit var storyAdapter: StoryAdapter
    private var reload = false
    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val isSucceeds = data?.getBooleanExtra(IntentExtras.RESULT_DATA, false)
            // xử lí khi mà thành công
            if (isSucceeds == true) {
                homeViewModel.getAllStories()
            }
        }
    }

    // Nhận sự kiện đăng Story bằng Video
    private val uploadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val result = intent?.getBooleanExtra(IntentExtras.RESULT_DATA_STR_VIDEO, false)
            if (result == true) {
                homeViewModel.getAllStories()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).showLoading()

        // Khởi tạo adapter 1 lần
        SharedPrefer.updateContext(requireContext())
        storyAdapter = StoryAdapter(mutableListOf(Story()), this, lifecycleScope)
        postAdapter = PostAdapter(mutableListOf(), lifecycleScope, this, storyAdapter)
        binding.rvHome.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            itemAnimator = DefaultItemAnimator().apply {
                addDuration = 400
                removeDuration = 400
                moveDuration = 400
                changeDuration = 400
            }
            setHasFixedSize(true)
            adapter = postAdapter
        }

        // Quan sát dữ liệu bài viết
        homeViewModel.posts.observe(viewLifecycleOwner) { data ->
            (activity as MainActivity).hideLoading()
            // cần 1 biến để check xem là có phải là đang load lại không ấy
            if (reload) {
                postAdapter.updateData(data.first)
                reload = false
            } else {
                if (data.first.isNotEmpty()) {
                    // Gộp bài viết mới và bài viết cũ
                    postAdapter.addPosts(data.first) // 👈 chỉ thêm mới, không replace
                } else {
                    Toast.makeText(
                        requireContext(),
                        data.second.ifEmpty { "Đã có lỗi xảy ra" },
                        Toast.LENGTH_SHORT
                    ).show()
                }
                binding.swipeRefresh.isRefreshing = false
            }
        }

        homeViewModel.fetchFirstPage()
        homeViewModel.getAllStories()

        binding.rvHome.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)

                // direction = 1 nghĩa là kiểm tra có thể cuộn tiếp xuống không
                if (!rv.canScrollVertically(1)) {
                    // => Đã cuộn đến cuối danh sách
                    homeViewModel.fetchNextPage()
                }
            }
        })

        // Load lại dữ liệu
        parentFragmentManager.setFragmentResultListener(
            "request_post_added",
            viewLifecycleOwner
        ) { _, bundle ->
            val isAdded = bundle.getBoolean("isAdded")
            if (isAdded) {
                // Gọi lại API load danh sách bài viết
                homeViewModel.refresh()
                reload = true
            }
        }

        // 🔹 Refresh để load lại
        binding.swipeRefresh.setOnRefreshListener {
            homeViewModel.refresh()
        }

        // hiển thị giao diện Story
        homeViewModel.story.observe(viewLifecycleOwner) {
            Log.d("CheckListStory", it.toString())
            storyAdapter.updateListStory(it)
            postAdapter.notifyItemChanged(0)
        }
    }

    // Chuyển sang màn khác
    // Kiểm tra xem có phải là mình ko nếu mà là mình thì chuyển sang bên trang Fragment MyProfile
    override fun onAvatarClick(user: User?) {
        if (user?.id != SharedPrefer.getId()) {
            val profileFragment = ProfileFragment()
            val bundle = Bundle().apply {
                putParcelable(IntentExtras.EXTRA_USER, user)
            }
            profileFragment.arguments = bundle
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.setSlideAnimations()
            transaction.add(R.id.fragment, profileFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        } else {
            // Chuyển sang bên trang đó là trang MyProfile
            (activity as MainActivity).switchScreenMyProfile()
        }
    }

    override fun onStoryClick() {
        val intent = Intent(requireActivity(), StoryActivity::class.java)
        launcher.launch(intent)
    }

    override fun switchScreenStory(listStory: List<Story>) {
        val intent = Intent(requireActivity(), ViewStoryActivity::class.java)
        intent.putParcelableArrayListExtra(IntentExtras.EXTRA_DATA_STORY, ArrayList(listStory))
        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            requireContext(),
            uploadReceiver,
            IntentFilter("com.example.easymedia.UPLOAD_DONE"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(uploadReceiver)
    }
}

interface OnAvatarClickListener {
    fun onAvatarClick(user: User?)
    fun onStoryClick()
    fun switchScreenStory(listStory: List<Story>)
}


