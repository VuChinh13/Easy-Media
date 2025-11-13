package com.example.easymedia.ui.component.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.easymedia.R
import com.example.easymedia.data.model.User
import com.example.easymedia.databinding.FragmentProfileBinding
import com.example.easymedia.ui.component.main.MainActivity
import com.example.easymedia.ui.component.profile.adapter.ProfileAdapter
import com.example.easymedia.ui.component.utils.IntentExtras

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private val myProfileViewModel: ProfileViewModel by viewModels()
    private lateinit var postAdapter: ProfileAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as MainActivity).hideLoading()
        // Nhận dữ liệu khi mà truyền sang
        val user = arguments?.getParcelable<User>(IntentExtras.EXTRA_USER)
        myProfileViewModel.getUserPosts(user!!.id)

        binding.shimmerUsername.startShimmer()
        // hiển thị lên trên UI
        binding.tvName.text = user.fullName
        binding.tvTotalPost.text = user.postCount.toString()
        binding.tvIntroduce.text = user.bio
        binding.tvUsername.text = user.username
        binding.tvTotalFollowers.text = user.followers.toString()
        binding.tvTotalFollowing.text = user.following.toString()
        Glide.with(this).load(user.profilePicture).error(R.drawable.ic_avatar)
            .into(binding.ivAvatar)

        postAdapter = ProfileAdapter(requireActivity(), mutableListOf())
        binding.rvMyPost.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvMyPost.adapter = postAdapter
        postAdapter.updateUser(user)

        // Lắng nghe sự kiện khi mà lấy được tất cả bài viết của người dùng
        myProfileViewModel.getUserPostsResult.observe(viewLifecycleOwner) { result ->
            binding.shimmerUsername.stopShimmer()
            binding.shimmerUsername.visibility = View.INVISIBLE
            if (result.isNotEmpty()) {
                postAdapter.updateListPost(result)
            }
        }

        // khi mà quay về thì làm thế nào nhể hay là chỉ là quay về trang trước như là bình thường
        binding.btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}

