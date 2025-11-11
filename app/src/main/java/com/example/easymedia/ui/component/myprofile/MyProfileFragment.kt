package com.example.easymedia.ui.component.myprofile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.easymedia.R
import com.example.easymedia.data.model.User
import com.example.easymedia.databinding.FragmentMyProfileBinding
import com.example.easymedia.ui.component.main.MainActivity
import com.example.easymedia.ui.component.myprofile.adapter.MyPostAdapter
import com.example.easymedia.ui.component.splash.SplashActivity
import com.example.easymedia.ui.component.utils.SharedPrefer
import androidx.core.content.edit
import com.example.easymedia.data.model.Post
import com.example.easymedia.ui.component.addpost.AddPostFragment
import com.example.easymedia.ui.component.animation.FragmentTransactionAnimation.setSlideAnimations
import com.example.easymedia.ui.component.search.SearchFragment
import com.example.easymedia.ui.component.updateinformation.UpdateInformationFragment
import com.example.easymedia.ui.component.utils.IntentExtras

class MyProfileFragment : Fragment() {
    private lateinit var binding: FragmentMyProfileBinding
    private val myProfileViewModel: MyProfileViewModel by viewModels()
    private var inforUserResponse: User? = null
    private lateinit var myPostAdapter: MyPostAdapter
    private var listPost = mutableListOf<Post>()

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
        val uid = SharedPrefer.getId()
        myProfileViewModel.getInforUser(uid)
        myProfileViewModel.getUserPosts(uid)
        myPostAdapter =
            MyPostAdapter(requireActivity(), mutableListOf())


        // Chuyển sang chỉnh sửa Profile
        binding.btnEditProfile.setOnClickListener {
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

        myProfileViewModel.getUserPostsResult.observe(viewLifecycleOwner) { result ->
            if (result.isEmpty()) {
                binding.tvTitle1.visibility = View.VISIBLE
                binding.tvTitle2.visibility = View.VISIBLE
                binding.btnCreatePost.visibility = View.VISIBLE
                (activity as MainActivity).hideLoading()
            } else {
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
        }

        binding.ivLogout.setOnClickListener {
            val alertDialog =
                AlertDialog.Builder(requireContext(), android.R.style.Theme_Material_Dialog_Alert)
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
            alertDialog.show()
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
            val transactionMyProfileFragment = requireActivity().supportFragmentManager.beginTransaction()
            transactionMyProfileFragment.setSlideAnimations()
            transactionMyProfileFragment.add(R.id.fragment, searchFragment)
            transactionMyProfileFragment.addToBackStack(null)
            transactionMyProfileFragment.commit()
        }

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, callback)
    }

//    override fun onResume() {
//        super.onResume()
//        SharedPrefer.updateContext(requireContext())
//        val userName = SharedPrefer.getUserName()
//        myProfileViewModel.getInforUser(userName)
//        myProfileViewModel.getUserPosts(userName)
//    }
}


