package com.example.easymedia.ui.component.addpost

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.easymedia.databinding.FragmentAddPostBinding
import com.example.easymedia.ui.component.main.MainActivity
import com.example.easymedia.ui.component.utils.AppToast
import com.example.easymedia.ui.component.utils.SharedPrefer
import java.io.File
import java.io.FileOutputStream

class AddPostFragment : Fragment() {
    private val postImages: MutableList<File> = mutableListOf()
    private val addPostViewModel: AddPostViewModel by viewModels()
    private lateinit var binding: FragmentAddPostBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAddPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pickMultipleMedia =
            registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
                if (uris.isNotEmpty()) {
                    uris.forEach { uri ->
                        binding.ivPostImage.setImageURI(uri)
                        postImages.add(uriToFile(requireContext(), uri))
                    }
                    // Hiển thị nút đó sau khi mà chọn xong nếu mà có chọn ảnh
                    binding.btnClearSelection.visibility = View.VISIBLE
                }
            }

        binding.btChooseImage.setOnClickListener {
            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.ivClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        addPostViewModel.result.observe(viewLifecycleOwner) { result ->
            if (result.first) {
                (activity as MainActivity).hideLoading()
                // Nếu mà thành công
                Toast.makeText(
                    requireContext(),
                    "Đã chia sẻ thành công bài viết",
                    Toast.LENGTH_SHORT
                )
                    .show()
                parentFragmentManager.setFragmentResult(
                    "request_post_added",
                    bundleOf("isAdded" to true)
                )
                parentFragmentManager.popBackStack()
            } else {
                (activity as MainActivity).hideLoading()
                Toast.makeText(
                    requireContext(),
                    "Đã có lỗi xảy ra hãy kiểm tra lại",
                    Toast.LENGTH_SHORT
                )
                    .show()
                parentFragmentManager.popBackStack()
            }
        }

        binding.btShare.setOnClickListener {
            if (binding.etContent.text!!.isEmpty() || postImages.isEmpty()) {
                Toast.makeText(requireContext(), "Hãy thêm nội dung và ảnh để hoàn thiện bài viết", Toast.LENGTH_SHORT).show()
            } else {
                (activity as MainActivity).showLoading()
                SharedPrefer.updateContext(requireContext())
                val userId = SharedPrefer.getId()
                addPostViewModel.createPost(
                    userId,
                    binding.etContent.text.toString(),
                    null,
                    postImages
                )
            }

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

        // Sự kiện khi Clear
        binding.btnClearSelection.setOnClickListener {
            binding.ivPostImage.setImageDrawable(null)
            postImages.clear()
            binding.btnClearSelection.visibility = View.INVISIBLE
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as MainActivity).showBottomBar()
    }

    private fun uriToFile(context: Context, uri: Uri): File {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        val fileName = "image_${System.currentTimeMillis()}.jpg"
        val file = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file
    }
}