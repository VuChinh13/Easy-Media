//package com.example.instagram.ui.component.profile
//
//import android.content.Context
//import android.os.Bundle
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.fragment.app.viewModels
//import androidx.recyclerview.widget.DefaultItemAnimator
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.bumptech.glide.Glide
//import com.example.instagram.R
//import com.example.instagram.databinding.FragmentProfileBinding
//import com.example.instagram.ui.component.profile.adapter.ProfileAdapter
//import com.example.instagram.ui.component.utils.IntentExtras
//
//
//class ProfileFragment : Fragment() {
//    private lateinit var binding: FragmentProfileBinding
//    private val myProfileViewModel: ProfileViewModel by viewModels()
//    private lateinit var postAdapter: ProfileAdapter
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        binding = FragmentProfileBinding.inflate(layoutInflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//
//        val userName = arguments?.getString(IntentExtras.EXTRA_USER_NAME).toString()
//        myProfileViewModel.getInforUser(userName)
//        myProfileViewModel.getUserPosts(userName)
//
//        myProfileViewModel.getUserPostsResult.observe(viewLifecycleOwner) { result ->
//            if (result != null) {
//                if (result.data.data.isEmpty()) {
//                    binding.tvTitle1.visibility = View.VISIBLE
//                    binding.ivCamera.visibility = View.VISIBLE
//                } else {
//                    binding.tvTitle1.visibility = View.GONE
//                    binding.ivCamera.visibility = View.GONE
//
//                    val sharedPreferences =
//                        requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
//                    val userId = sharedPreferences.getString("_id", "") ?: ""
//                    val myUserName = sharedPreferences.getString("username", "") ?: ""
//
//                    postAdapter =
//                        ProfileAdapter(requireContext(), result.data.data, myUserName, userId)
//                    binding.rvPost.layoutManager = LinearLayoutManager(
//                        requireContext(),
//                        LinearLayoutManager.VERTICAL, false
//                    )
//                    val itemAnimator = DefaultItemAnimator().apply {
//                        addDuration = 400
//                        removeDuration = 400
//                        moveDuration = 400
//                        changeDuration = 400
//                    }
//                    binding.rvPost.itemAnimator = itemAnimator
//                    binding.rvPost.adapter = postAdapter
//                }
//            }
//        }
//
//        myProfileViewModel.getInforUserResult.observe(viewLifecycleOwner) { result ->
//            if (result != null) {
//                Glide.with(this).load(result.data.avatar)
//                    .error(R.drawable.ic_avatar)
//                    .into(binding.ivAvatar)
//                binding.tvName.text = result.data.name
//                binding.tvUsername.text = result.data.username
//                binding.tvTotalPost.text = result.data.totalPost.toString()
//                binding.tvIntroduce.text = result.data.introduce
//                binding.tvUsernameThread.text = result.data.username
//            } else {
//                Toast.makeText(
//                    requireContext(),
//                    "Đã xảy ra lỗi, hãy kiểm tra lại1",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//
//        binding.ivBackArrow.setOnClickListener {
//            parentFragmentManager.popBackStack()
//        }
//    }
//}
