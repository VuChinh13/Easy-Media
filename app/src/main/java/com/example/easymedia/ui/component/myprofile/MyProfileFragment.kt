package com.example.easymedia.ui.component.myprofile

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.easymedia.R
import com.example.easymedia.data.model.Post
import com.example.easymedia.data.model.Story
import com.example.easymedia.data.model.User
import com.example.easymedia.databinding.FragmentMyProfileBinding
import com.example.easymedia.ui.component.addpost.AddPostFragment
import com.example.easymedia.ui.component.animation.FragmentTransactionAnimation.setSlideAnimations
import com.example.easymedia.ui.component.follower.FollowerBottomSheet
import com.example.easymedia.ui.component.main.MainActivity
import com.example.easymedia.ui.component.myprofile.adapter.MyPostAdapter
import com.example.easymedia.ui.component.postdetail.PostDetailActivity
import com.example.easymedia.ui.component.profile.ProfileFragment
import com.example.easymedia.ui.component.splash.SplashActivity
import com.example.easymedia.ui.component.updateinformation.UpdateInformationFragment
import com.example.easymedia.ui.component.utils.IntentExtras
import com.example.easymedia.ui.component.utils.SharedPrefer
import com.example.easymedia.ui.component.following.FollowingBottomSheet
import com.example.easymedia.ui.component.myprofile.adapter.MyStoryAdapter
import com.example.easymedia.ui.component.viewstory.ViewStoryActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MyProfileFragment : Fragment() {
    private lateinit var binding: FragmentMyProfileBinding
    private val myProfileViewModel: MyProfileViewModel by viewModels()
    private var inforUserResponse: User? = null
    private lateinit var myPostAdapter: MyPostAdapter
    private lateinit var myStoryAdapter: MyStoryAdapter
    private var listPost = mutableListOf<Post>()
    private val uid = SharedPrefer.getId()

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val isSucceeds = data?.getBooleanExtra(IntentExtras.RESULT_DATA, false)
            if (isSucceeds == true) {
                // true có nghĩa là cập nhật cần load lại
                myProfileViewModel.getInforUser(uid)
                myProfileViewModel.getUserPosts(uid)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMyProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as MainActivity).fragmentCurrent = "MyProfileFragment"
        (activity as MainActivity).countFragment++
        (activity as MainActivity).showLoading()

        SharedPrefer.updateContext(requireContext())
        myProfileViewModel.getInforUser(uid)
        myProfileViewModel.getUserPosts(uid)
        myProfileViewModel.getStory(uid)
        myPostAdapter =
            MyPostAdapter(mutableListOf()) { user, position ->
                switchScreen(user, position)
            }

        myStoryAdapter =
            MyStoryAdapter(mutableListOf()) { list ->
                switchScreenStory(list)
            }

        // Chuyển sang chỉnh sửa Profile
        binding.ivUpdateInfor.setOnClickListener {
            (activity as MainActivity).showLoading()

            myProfileViewModel.getInforUserResult.value.let { user ->
                val bundle = Bundle().apply {
                    putString(IntentExtras.EXTRA_NAME, user?.fullName)
                    putString(IntentExtras.EXTRA_GENDER, user?.gender)
                    putString(IntentExtras.EXTRA_AVATAR, user?.profilePicture)
                    putString(
                        IntentExtras.EXTRA_INTRODUCE,
                        inforUserResponse?.bio
                    )
                    putString(IntentExtras.EXTRA_ADDRESS, user?.location)
                }

                val updateInformationFragment = UpdateInformationFragment()
                updateInformationFragment.arguments = bundle

                // chỗ này là chuyển màn hình
                requireActivity().supportFragmentManager.beginTransaction()
                    .add(R.id.fragment, updateInformationFragment)
                    .addToBackStack(null)
                    .commit()
            }

            // ẩn
            (activity as MainActivity).hideBottomBar()
        }

        binding.tvTotalFollowing.setOnClickListener {
            val followingSheet = FollowingBottomSheet(
                myProfileViewModel.getInforUserResult.value?.following ?: listOf(),
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
                myProfileViewModel.getInforUserResult.value?.followers ?: listOf(),
                { user ->
                    switchProfile(user)
                }
            )
            followingSheet.show(
                parentFragmentManager, // hoặc childFragmentManager
                "FollowerBottomSheet"
            )
        }

        myProfileViewModel.getStoryResult.observe(viewLifecycleOwner) { result ->
            myStoryAdapter.updateListStory(result)
            binding.rcvStory.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            binding.rcvStory.adapter = myStoryAdapter
        }

        myProfileViewModel.getUserPostsResult.observe(viewLifecycleOwner) { result ->
            if (result.isEmpty()) {
                binding.rvMyPost.visibility = View.GONE
                binding.tvTitle1.visibility = View.VISIBLE
                binding.tvTitle2.visibility = View.VISIBLE
                binding.btnCreatePost.visibility = View.VISIBLE
                (activity as MainActivity).hideLoading()
            } else {
                binding.rvMyPost.visibility = View.VISIBLE
                binding.tvTitle1.visibility = View.GONE
                binding.tvTitle2.visibility = View.GONE
                binding.btnCreatePost.visibility = View.GONE
                listPost.addAll(result)
                (activity as MainActivity).hideLoading()
                myPostAdapter.updateListPost(result)
                binding.rvMyPost.layoutManager = GridLayoutManager(requireContext(), 3)
                binding.rvMyPost.adapter = myPostAdapter
            }
        }

        // sau khi mà lấy được thông tin người dùng
        myProfileViewModel.getInforUserResult.observe(viewLifecycleOwner) { result ->
            inforUserResponse = result
            Glide.with(this).load(result?.profilePicture)
                .error(R.drawable.ic_avatar)
                .into(binding.ivAvatar)
            myPostAdapter.updateUser(result)
            binding.tvName.text = result?.fullName
            binding.tvUsername.text = result?.username
            binding.tvTotalPost.text = result?.postCount.toString()
            binding.tvIntroduce.text = result?.bio
            binding.tvTotalFollowing.text = result?.following?.size.toString()
            binding.tvTotalFollowers.text = result?.followers?.size.toString()
        }

        binding.ivLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext(), R.style.MyAlertDialogTheme)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất tài khoản?")
                .setPositiveButton("Đồng ý") { dialog, _ ->
                    // Clear dữ liệu
                    SharedPrefer.getSharedPrefer().edit { clear() }
                    val intent = Intent(requireActivity(), SplashActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    dialog.dismiss()
                    parentFragmentManager.popBackStack()
                }
                .setNegativeButton("Hủy") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        // handle button back
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // update current fragment
                (activity as MainActivity).apply {
                    fragmentCurrent = fragmentPre
                    countFragment--
                }

                // pop fragment
                requireActivity()
                    .supportFragmentManager
                    .popBackStack()
            }
        }

        // Taọ bài viết khi mà chưa có bài viết nảo cả
        binding.btnCreatePost.setOnClickListener {
            val searchFragment = AddPostFragment()
            val transactionMyProfileFragment =
                requireActivity().supportFragmentManager.beginTransaction()
            transactionMyProfileFragment.setSlideAnimations()
            transactionMyProfileFragment.add(R.id.fragment, searchFragment)
            transactionMyProfileFragment.addToBackStack(null)
            transactionMyProfileFragment.commit()
        }

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, callback)
    }

    fun switchScreen(user: User, position: Int) {
        val intent = Intent(context, PostDetailActivity::class.java)
        intent.putExtra(IntentExtras.EXTRA_USER, user)
        intent.putExtra(IntentExtras.EXTRA_POSITION, position)
        launcher.launch(intent)
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

    fun switchScreenStory(listStory: List<Story>) {
        val intent = Intent(requireActivity(), ViewStoryActivity::class.java)
        intent.putParcelableArrayListExtra(IntentExtras.EXTRA_DATA_STORY, ArrayList(listStory))
        startActivity(intent)
    }


//    override fun onResume() {
//        super.onResume()
//        SharedPrefer.updateContext(requireContext())
//        val userName = SharedPrefer.getUserName()
//        myProfileViewModel.getInforUser(userName)
//        myProfileViewModel.getUserPosts(userName)
//    }
}


