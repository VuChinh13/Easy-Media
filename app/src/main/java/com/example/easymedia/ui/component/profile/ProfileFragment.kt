package com.example.easymedia.ui.component.profile

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.easymedia.R
import com.example.easymedia.data.model.User
import com.example.easymedia.data.repository.AuthRepositoryImpl
import com.example.easymedia.data.data_source.firebase.FirebaseAuthService
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.databinding.FragmentProfileBinding
import com.example.easymedia.ui.component.animation.FragmentTransactionAnimation.setSlideAnimations
import com.example.easymedia.ui.component.follower.FollowerBottomSheet
import com.example.easymedia.ui.component.main.MainActivity
import com.example.easymedia.ui.component.profile.adapter.ProfileAdapter
import com.example.easymedia.ui.component.utils.IntentExtras
import com.example.easymedia.ui.component.utils.SharedPrefer
import com.example.easymedia.ui.component.following.FollowingBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private val myProfileViewModel: ProfileViewModel by viewModels()
    private lateinit var postAdapter: ProfileAdapter
    val currentUserId = SharedPrefer.getId()

    // Repository
    private val authRepository =
        AuthRepositoryImpl(FirebaseAuthService(CloudinaryServiceImpl()))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as MainActivity).hideLoading()

        // Lấy user được truyền qua arguments
        val user = arguments?.getParcelable<User>(IntentExtras.EXTRA_USER)
        if (user == null) {
            Toast.makeText(requireContext(), "Lỗi: User không tồn tại", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        // gọi load posts
        myProfileViewModel.getUserPosts(user.id)

        // shimmer
        binding.shimmerUsername.startShimmer()

        // Set user lên UI
        binding.tvName.text = user.fullName
        binding.tvTotalPost.text = user.postCount.toString()
        binding.tvIntroduce.text = user.bio
        binding.tvUsername.text = user.username
        binding.tvTotalFollowers.text = user.followers.size.toString()
        binding.tvTotalFollowing.text = user.following.size.toString()

        Glide.with(this)
            .load(user.profilePicture)
            .error(R.drawable.ic_avatar)
            .into(binding.ivAvatar)

        // Cho phép thay đổi trạng thái follow
        var isFollowing = user.followers.contains(currentUserId)

        fun updateFollowUI() {
            if (isFollowing) {
                binding.btnFollow.text = "Đang theo dõi"
                binding.btnFollow.setBackgroundResource(R.drawable.bg_button_myprofile)
                binding.btnFollow.setTextColor(Color.BLACK)
            } else {
                binding.btnFollow.text = "Theo dõi"
                binding.btnFollow.setBackgroundResource(R.drawable.bg_button_following)
                binding.btnFollow.setTextColor(Color.WHITE)
            }

            binding.tvTotalFollowers.text = user.followers.size.toString()
            binding.tvTotalFollowing.text = user.following.size.toString()
        }

        updateFollowUI()

        // Xử lý nút Follow
        binding.btnFollow.setOnClickListener {
            binding.btnFollow.isEnabled = false // chống double click
            lifecycleScope.launch {
                try {
                    if (isFollowing) {
                        // UNFOLLOW
                        isFollowing = false
                        user.followers = user.followers.filter { it != currentUserId }
                        updateFollowUI()
                        withContext(Dispatchers.IO) {
                            authRepository.removeFollowing(currentUserId, user.id)
                        }

                    } else {
                        // FOLLOW
                        isFollowing = true
                        user.followers = user.followers.toMutableList().apply {
                            add(currentUserId)
                        }
                        updateFollowUI()
                        withContext(Dispatchers.IO) {
                            authRepository.addFollowing(currentUserId, user.id)
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    binding.btnFollow.isEnabled = true
                }
            }
        }

        binding.tvTotalFollowing.setOnClickListener {
            val followingSheet = FollowingBottomSheet(
                user.following
            ) { user ->
                switchProfile(user)
            }
            followingSheet.show(
                parentFragmentManager, // hoặc childFragmentManager
                "FollowingBottomSheet"
            )
        }

        binding.tvTotalFollowers.setOnClickListener {
            val followingSheet = FollowerBottomSheet(
                user.followers
            ) { user ->
                switchProfile(user)
            }
            followingSheet.show(
                parentFragmentManager, // hoặc childFragmentManager
                "FollowerBottomSheet"
            )
        }

        // RecyclerView Posts
        postAdapter = ProfileAdapter(requireActivity(), mutableListOf())
        binding.rvMyPost.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvMyPost.adapter = postAdapter
        postAdapter.updateUser(user)

        // Lắng nghe kết quả load posts
        myProfileViewModel.getUserPostsResult.observe(viewLifecycleOwner) { result ->
            binding.shimmerUsername.stopShimmer()
            binding.shimmerUsername.visibility = View.INVISIBLE
            if (result.isNotEmpty()) {
                postAdapter.updateListPost(result)
            }
        }

        // Quay về
        binding.btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    fun switchProfile(user: User) {
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
    }
}
