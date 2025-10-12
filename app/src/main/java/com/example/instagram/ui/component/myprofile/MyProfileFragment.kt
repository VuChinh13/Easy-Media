package com.example.instagram.ui.component.myprofile

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
import com.example.instagram.R
import com.example.instagram.data.model.User
import com.example.instagram.databinding.FragmentMyProfileBinding
import com.example.instagram.ui.component.main.MainActivity
import com.example.instagram.ui.component.myprofile.adapter.MyPostAdapter
import com.example.instagram.ui.component.splash.SplashActivity
import com.example.instagram.ui.component.utils.SharedPrefer
import androidx.core.content.edit

class MyProfileFragment : Fragment() {
    private lateinit var binding: FragmentMyProfileBinding
    private val myProfileViewModel: MyProfileViewModel by viewModels()
    private var inforUserResponse: User? = null
    private lateinit var myPostAdapter: MyPostAdapter

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
        (activity as MainActivity).getBinding().loadingOverlay.visibility = View.VISIBLE

        SharedPrefer.updateContext(requireContext())
        val uid = SharedPrefer.getId()
        myProfileViewModel.getInforUser(uid)
        myProfileViewModel.getUserPosts(uid)

//        binding.btnEditProfile.setOnClickListener {
//            val bundle = Bundle().apply {
//                putString(IntentExtras.EXTRA_NAME, inforUserResponse?.fullName.toString()
//                putString(IntentExtras.EXTRA_GENDER, inforUserResponse?.?.gender.toString())
//                putString(IntentExtras.EXTRA_AVATAR, inforUserResponse?.data?.avatar.toString())
//                putString(
//                    IntentExtras.EXTRA_INTRODUCE,
//                    inforUserResponse?.data?.introduce.toString()
//                )
//                putString(IntentExtras.EXTRA_ADDRESS, inforUserResponse?.data?.address.toString())
//            }
//
//            val updateInformationFragment = UpdateInformationFragment()
//            updateInformationFragment.arguments = bundle
//
//            requireActivity().supportFragmentManager.beginTransaction()
//                .add(R.id.fragment, updateInformationFragment)
//                .addToBackStack(null)
//                .commit()
//        }

        myProfileViewModel.getUserPostsResult.observe(viewLifecycleOwner) { result ->
            if (result.isEmpty()) {
                binding.tvTitle1.visibility = View.VISIBLE
                binding.tvTitle2.visibility = View.VISIBLE
                binding.btnTao.visibility = View.VISIBLE
            } else {
                binding.tvTitle1.visibility = View.GONE
                binding.tvTitle2.visibility = View.GONE
                binding.btnTao.visibility = View.GONE
                (activity as MainActivity).getBinding().loadingOverlay.visibility = View.GONE
                myPostAdapter =
                    MyPostAdapter(requireActivity(), result)
                binding.rvMyPost.layoutManager = GridLayoutManager(requireContext(), 3)
                binding.rvMyPost.adapter = myPostAdapter
            }
        }

        myProfileViewModel.getInforUserResult.observe(viewLifecycleOwner) { result ->
            inforUserResponse = result
            Glide.with(this).load(result?.profilePicture)
                .error(R.drawable.ic_avatar)
                .into(binding.ivAvatar)
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


