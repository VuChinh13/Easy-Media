package com.example.easymedia.ui.component.search

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.easymedia.R
import com.example.easymedia.data.model.User
import com.example.easymedia.databinding.FragmentSearchBinding
import com.example.easymedia.ui.component.main.MainActivity
import com.example.easymedia.ui.component.profile.ProfileFragment
import com.example.easymedia.ui.component.search.adapter.HistoryAdapter
import com.example.easymedia.ui.component.search.adapter.SearchAutoCompleteAdapter
import com.example.easymedia.ui.utils.IntentExtras
import com.example.easymedia.ui.utils.SharedPrefer

class SearchFragment : Fragment() {
    private lateinit var binding: FragmentSearchBinding
    private lateinit var adapter: SearchAutoCompleteAdapter
    private lateinit var adapterHistory: HistoryAdapter
    private val searchViewModel: SearchViewModel by viewModels()
    private lateinit var historyManager: SearchHistoryManager
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as MainActivity).apply {
            fragmentCurrent = "SearchFragment"
            countFragment++
        }
        historyManager = SearchHistoryManager(requireContext())

        val listHistory = historyManager.getSearchHistory()
        adapterHistory = HistoryAdapter(
            listHistory.toMutableList(),
            clearItemHistory = { userId ->
                historyManager.removeUserFromHistory(userId)
            },
            listener = { user ->
                switchScreen(user)
            },
            (activity as MainActivity)
        )

        binding.rcvHistoryUser.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            itemAnimator = DefaultItemAnimator().apply {
                addDuration = 400
                removeDuration = 400
                moveDuration = 400
                changeDuration = 400
            }
            setHasFixedSize(true)
            adapter = adapterHistory
        }

        adapter = SearchAutoCompleteAdapter(requireContext())
        binding.actvUser.dropDownVerticalOffset = 8.dpToPx(requireContext())
        binding.actvUser.dropDownHorizontalOffset = 4.dpToPx(requireContext())
        binding.actvUser.setAdapter(adapter)
        binding.actvUser.threshold = 1


        // khi ViewModel có dữ liệu:
        searchViewModel.getInforUserResults.observe(viewLifecycleOwner) { users ->
            adapter.replace(users)
        }

        // 3) Gọi lấy data SAU khi đã set observer
        searchViewModel.getListUser()

        // 4) Bắt sự kiện chọn item
        binding.actvUser.setOnItemClickListener { _, _, position, _ ->
            val user = adapter.getItem(position) ?: return@setOnItemClickListener
            // Điền text mà không kích hoạt filter lại
            binding.actvUser.setText(user.username, false)

            hideKeyboard()
            // Lưu vào lịch sử:
            historyManager.saveUserToHistory(user)
            adapterHistory.addUser(user)
            // Chú ý đoạn chuyển màn này
            if (user.id != SharedPrefer.getId()) {
                switchScreen(user)
            } else (activity as MainActivity).switchScreenMyProfile()
        }

        // 5) Back button
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (activity as MainActivity).apply {
                    fragmentCurrent = fragmentPre
                    countFragment--
                }
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        binding.btnClose.setOnClickListener {
            (activity as MainActivity).apply {
                fragmentCurrent = fragmentPre
                countFragment--
            }
            requireActivity().supportFragmentManager.popBackStack()
        }

        searchViewModel.getUserById.observe(viewLifecycleOwner) { user ->
            val profileFragment = ProfileFragment()
            val transactionProfileFragment =
                (activity as MainActivity).supportFragmentManager.beginTransaction()
            val bundle = Bundle().apply {
                putParcelable(IntentExtras.EXTRA_USER, user)
            }
            profileFragment.arguments = bundle
            transactionProfileFragment.add(R.id.fragment, profileFragment)
            transactionProfileFragment.addToBackStack(null)
            transactionProfileFragment.commit()
        }
    }

    fun Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }

    @SuppressLint("ServiceCast")
    fun Context.hideKeyboard(view: View) {
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun switchScreen(user: User) {
        (activity as MainActivity).showLoading()
        searchViewModel.getUserById(user)
    }
}

fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}
