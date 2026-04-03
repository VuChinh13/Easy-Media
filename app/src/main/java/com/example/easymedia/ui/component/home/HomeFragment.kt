package com.example.easymedia.ui.component.home

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.easymedia.R
import com.example.easymedia.base.BaseFragment
import com.example.easymedia.data.model.Location
import com.example.easymedia.data.model.Post
import com.example.easymedia.data.model.Story
import com.example.easymedia.data.model.User
import com.example.easymedia.databinding.FragmentHomeBinding
import com.example.easymedia.extension.showToast
import com.example.easymedia.ui.component.animation.FragmentTransactionAnimation.setSlideAnimations
import com.example.easymedia.ui.component.home.adapter.PostAdapter
import com.example.easymedia.ui.component.home.adapter.StoryAdapter
import com.example.easymedia.ui.component.main.MainActivity
import com.example.easymedia.ui.component.mapdetail.MapDetailActivity
import com.example.easymedia.ui.component.profile.ProfileFragment
import com.example.easymedia.ui.component.story.StoryActivity
import com.example.easymedia.ui.component.viewstory.ViewStoryActivity
import com.example.easymedia.utils.IntentExtras
import com.example.easymedia.utils.SharedPrefer

class HomeFragment() : BaseFragment<FragmentHomeBinding, HomeViewModel>(), OnAvatarClickListener {
    override val viewModel: HomeViewModel by viewModels()
    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    private lateinit var postAdapter: PostAdapter
    private var isNotification = false
    private lateinit var storyAdapter: StoryAdapter
    private var reload = false
    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val isSucceeds = data?.getBooleanExtra(IntentExtras.RESULT_DATA, false)
            if (isSucceeds == true) {
                viewModel.getAllStories()
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    val launcherStory = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val user = data?.getParcelableExtra<User>(IntentExtras.EXTRA_USER)
            if (user != null) {
                onAvatarClick2(user)
            }

            val result = data?.getBooleanExtra(IntentExtras.RESULT_DATA, false)
            if (result == true) {
                viewModel.getAllStories()
            }
        }
    }
    private val uploadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val result = intent?.getBooleanExtra(IntentExtras.RESULT_DATA_STR, false)
            if (result == true) {
                isNotification = true
                viewModel.getAllStories()
            }
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        (activity as MainActivity).showLoading()
        initAdapter()
        listenerAdapter()
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
            viewModel.getAllStories()
        }
    }

    override fun initData() {
        super.initData()
        viewModel.posts.observe(viewLifecycleOwner) { data ->
            (activity as MainActivity).hideLoading()
            if (reload) {
                postAdapter.updateData(data.first)
                reload = false
            } else {
                if (data.first.isNotEmpty()) {
                    postAdapter.addPosts(data.first)
                } else {
                    showToast(getString(R.string.dialog_error))
                }
                binding.swipeRefresh.isRefreshing = false
            }
        }
        viewModel.fetchFirstPage()
        viewModel.getAllStories()


        parentFragmentManager.setFragmentResultListener(
            "request_post_added",
            viewLifecycleOwner
        ) { _, bundle ->
            val isAdded = bundle.getBoolean("isAdded")
            if (isAdded) {
                viewModel.refresh()
                reload = true
            }
        }


        viewModel.story.observe(viewLifecycleOwner) {
            if (isNotification) {
                isNotification = false
                showToast(getString(R.string.post_published_successfully))
            }
            storyAdapter.updateListStory(it)
            postAdapter.notifyItemChanged(0)
        }
    }

    private fun initAdapter() {
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
    }

    private fun listenerAdapter() {
        binding.rvHome.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                if (!rv.canScrollVertically(1)) {
                    viewModel.fetchNextPage()
                }
            }
        })
    }

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
            (activity as MainActivity).switchScreenMyProfile()
        }
    }

    override fun onAvatarClick2(user: User?) {
        if (user?.id != SharedPrefer.getId()) {
            val profileFragment = ProfileFragment()
            val bundle = Bundle().apply {
                putParcelable(IntentExtras.EXTRA_USER, user)
            }
            profileFragment.arguments = bundle
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.add(R.id.fragment, profileFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        } else {
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
        launcherStory.launch(intent)
    }

    override fun switchScreenMapDetail(location: Location, post: Post) {
        val intent =
            Intent(requireContext(), MapDetailActivity::class.java)
        intent.putExtra("lat", location.latitude)
        intent.putExtra("lng", location.longitude)
        intent.putExtra("name", location.address)
        intent.putExtra(IntentExtras.EXTRA_USER, post)
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
    fun onAvatarClick2(user: User?)
    fun onStoryClick()
    fun switchScreenStory(listStory: List<Story>)
    fun switchScreenMapDetail(location: Location, post: Post)
}


