//package com.example.instagram.ui.component.search
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.activity.OnBackPressedCallback
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.viewModels
//import com.example.instagram.R
//import com.example.instagram.databinding.FragmentSearchBinding
//import com.example.instagram.ui.component.animation.FragmentTransactionAnimation.setSlideAnimations
//import com.example.instagram.ui.component.main.MainActivity
//import com.example.instagram.ui.component.profile.ProfileFragment
//import com.example.instagram.ui.component.search.adapter.SearchAutoCompleteAdapter
//
//class SearchFragment : Fragment() {
//    private lateinit var binding: FragmentSearchBinding
//    private lateinit var adapter: SearchAutoCompleteAdapter
//    private val searchViewModel: SearchViewModel by viewModels()
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        binding = FragmentSearchBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        (activity as MainActivity).apply {
//            fragmentCurrent = "SearchFragment"
//            countFragment++
//        }
//
//        adapter = SearchAutoCompleteAdapter(requireContext())
//        binding.actvUser.setAdapter(adapter)
//        binding.actvUser.threshold = 1
//
//        // khi ViewModel có dữ liệu:
//        searchViewModel.getInforUserResults.observe(viewLifecycleOwner) { users ->
//            adapter.replace(users)
//        }
//
//        // 3) Gọi lấy data SAU khi đã set observer
//        searchViewModel.getListUser()
//
//        // 4) Bắt sự kiện chọn item
//        binding.actvUser.setOnItemClickListener { _, _, position, _ ->
//            val u = adapter.getItem(position) ?: return@setOnItemClickListener
//            // Điền text mà không kích hoạt filter lại
//            binding.actvUser.setText(u.username ?: u.name.orEmpty(), false)
//            // TODO: điều hướng tới trang profile, v.v…
//
//            // Chú ý đoạn chuyển màn này
//            val profileFragment = ProfileFragment()
//            val transactionProfileFragment =
//                (activity as MainActivity).supportFragmentManager.beginTransaction()
//            transactionProfileFragment.setSlideAnimations()
//            transactionProfileFragment.add(R.id.fragment, profileFragment)
//            transactionProfileFragment.addToBackStack(null)
//            transactionProfileFragment.commit()
//        }
//
//        // 5) Back button
//        val callback = object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                (activity as MainActivity).apply {
//                    fragmentCurrent = fragmentPre
//                    countFragment--
//                }
//                requireActivity().supportFragmentManager.popBackStack()
//            }
//        }
//        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
//    }
//}
