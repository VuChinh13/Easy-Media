package com.example.easymedia.ui.component.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.easymedia.R
import com.example.easymedia.data.model.User
import com.example.easymedia.databinding.FragmentHomeBinding
import com.example.easymedia.ui.component.animation.FragmentTransactionAnimation.setSlideAnimations
import com.example.easymedia.ui.component.home.adapter.OnAvatarClickListener
import com.example.easymedia.ui.component.home.adapter.PostAdapter
import com.example.easymedia.ui.component.main.MainActivity
import com.example.easymedia.ui.component.profile.ProfileFragment
import com.example.easymedia.ui.component.story.StoryActivity
import com.example.easymedia.ui.component.utils.IntentExtras
import com.example.easymedia.ui.component.utils.SharedPrefer

/**
 * Xử lí việc phân trang
 *  - Hiển thị chỉ hiển thị hết các bài viết như bình thường nếu mà người mình theo dõi thì
 *    hiển thị trước
 */
class HomeFragment : Fragment(), OnAvatarClickListener {
    private lateinit var binding: FragmentHomeBinding
    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var postAdapter: PostAdapter
    private var reload = false

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
        postAdapter = PostAdapter(mutableListOf(), test(), lifecycleScope, this, this)
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
        startActivity(intent)
    }
}

fun test(): List<User> {
    val testUsers = listOf(
        User(
            id = "u1",
            username = "alice",
            fullName = "Alice Nguyen",
            profilePicture = "https://randomuser.me/api/portraits/women/1.jpg"
        ),
        User(
            id = "u2",
            username = "bob",
            fullName = "Bob Tran",
            profilePicture = "https://randomuser.me/api/portraits/men/2.jpg"
        ),
        User(
            id = "u3",
            username = "charlie",
            fullName = "Charlie Le",
            profilePicture = "https://randomuser.me/api/portraits/men/3.jpg"
        ),
        User(
            id = "u4",
            username = "daisy",
            fullName = "Daisy Pham",
            profilePicture = "https://randomuser.me/api/portraits/women/4.jpg"
        ),
        User(
            id = "u5",
            username = "edward",
            fullName = "Edward Hoang",
            profilePicture = "https://randomuser.me/api/portraits/men/5.jpg"
        ),
        User(
            id = "u6",
            username = "fiona",
            fullName = "Fiona Nguyen",
            profilePicture = "https://randomuser.me/api/portraits/women/6.jpg"
        ),
        User(
            id = "u7",
            username = "george",
            fullName = "George Phan",
            profilePicture = "https://randomuser.me/api/portraits/men/7.jpg"
        ),
        User(
            id = "u8",
            username = "hannah",
            fullName = "Hannah Vo",
            profilePicture = "https://randomuser.me/api/portraits/women/8.jpg"
        ),
        User(
            id = "u9",
            username = "ivan",
            fullName = "Ivan Bui",
            profilePicture = "https://randomuser.me/api/portraits/men/9.jpg"
        ),
        User(
            id = "u10",
            username = "julia",
            fullName = "Julia Dang",
            profilePicture = "https://randomuser.me/api/portraits/women/10.jpg"
        )
    )
    return testUsers
}

