package com.example.easymedia.ui.component.addpost

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import com.example.easymedia.R
import com.example.easymedia.base.BaseFragment
import com.example.easymedia.data.model.Location
import com.example.easymedia.databinding.FragmentAddPostBinding
import com.example.easymedia.extension.observe
import com.example.easymedia.extension.showToast
import com.example.easymedia.ui.component.main.MainActivity
import com.example.easymedia.ui.component.map.MapStoryActivity
import com.example.easymedia.utils.Files
import com.example.easymedia.utils.IntentExtras
import com.example.easymedia.utils.SharedPrefer
import java.io.File

class AddPostFragment() : BaseFragment<FragmentAddPostBinding, AddPostViewModel>() {
    override val viewModel: AddPostViewModel by viewModels()
    private val postImages: MutableList<File> = mutableListOf()
    private val userId by lazy {
        with(SharedPrefer) {
            updateContext(requireContext())
            getId()
        }
    }
    private var location: Location? = null
    private val pickMultipleMedia by lazy {
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
            if (uris.isNotEmpty()) {
                uris.forEach { uri ->
                    binding.ivPostImage.setImageURI(uri)
                    postImages.add(Files.uriToFile(requireContext(), uri))
                }
                binding.btnClearSelection.visibility = View.VISIBLE
            }
        }
    }
    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val location = result.data?.getParcelableExtra<Location>(IntentExtras.RESULT_DATA)
            this.location = location
            if (location != null) {
                showLocation()
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAddPostBinding {
        return FragmentAddPostBinding.inflate(inflater, container, false)
    }

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        with(binding) {
            btChooseImage.setOnClickListener {
                pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            ivClose.setOnClickListener {
                parentFragmentManager.popBackStack()
            }
            btnLocation.setOnClickListener {
                val intent = Intent(requireContext(), MapStoryActivity::class.java)
                launcher.launch(intent)
            }
            btShare.setOnClickListener {
                if (etContent.text!!.isEmpty() || postImages.isEmpty()) {
                    showToast(R.string.dialog_add_content_and_image)
                } else {
                    (activity as MainActivity).showLoading()
                    viewModel.createPost(
                        userId,
                        etContent.text.toString(),
                        location,
                        postImages
                    )
                }
            }
            btnClearSelection.setOnClickListener {
                ivPostImage.setImageDrawable(null)
                postImages.clear()
                btnClearSelection.visibility = View.INVISIBLE
            }
        }

        handleEventBack()
    }

    override fun initData() {
        super.initData()
        viewModel.event.observe(this@AddPostFragment) { value ->
            (activity as MainActivity).hideLoading()
            when (value) {
                is AddPostEvent.Error -> handleFail()
                is AddPostEvent.Success -> handleSuccess()
            }
        }
    }

    private fun handleSuccess() {
        showToast(R.string.dialog_share_post_success)
        with(parentFragmentManager) {
            setFragmentResult(
                "request_post_added",
                bundleOf("isAdded" to true)
            )
            beginTransaction()
                .remove(this@AddPostFragment)
                .commit()
            popBackStackImmediate()
        }
    }

    private fun handleFail() {
        showToast(R.string.dialog_error)
        with(parentFragmentManager) {
            beginTransaction()
                .remove(this@AddPostFragment)
                .commit()
            popBackStackImmediate()
        }
    }

    private fun handleEventBack() {
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

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as MainActivity).showBottomBar()
    }


    private fun showLocation() {
        with(binding) {
            icLocation.visibility = View.VISIBLE
            tvLocation.text = location?.address
            tvLocation.visibility = View.VISIBLE
        }
    }
}