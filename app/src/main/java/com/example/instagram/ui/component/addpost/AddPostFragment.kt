package com.example.instagram.ui.component.addpost

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.instagram.databinding.FragmentAddPostBinding
import com.example.instagram.ui.component.main.MainActivity
import com.example.instagram.ui.component.utils.AppToast
import com.example.instagram.ui.component.utils.SharedPrefer
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

        (activity as MainActivity).fragmentCurrent = "AddPostFragment"
        (activity as MainActivity).countFragment++

        val pickMultipleMedia =
            registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
                if (uris.isNotEmpty()) {
                    uris.forEach { uri ->
                        binding.ivPostImage.setImageURI(uri)
                        postImages.add(uriToFile(requireContext(), uri))
                    }
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
                (activity as MainActivity).getBinding().loadingOverlay.visibility = View.GONE // ẩn sau khi xong
                // Nếu mà thành công
                Toast.makeText(
                    requireContext(),
                    "Đã chia sẻ thành công bài viết",
                    Toast.LENGTH_SHORT
                )
                    .show()
                parentFragmentManager.popBackStack()
            } else {
                (activity as MainActivity).getBinding().loadingOverlay.visibility = View.GONE // ẩn sau khi xong
                Toast.makeText(
                    requireContext(),
                    "Đã có lỗi xảy ra hãy kiểm tra lại",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }

        binding.btShare.setOnClickListener {
            if (binding.etContent.text.isEmpty()) {
                AppToast.show(requireContext(), "Hãy nhập nội dung bài viết")
            } else {
                (activity as MainActivity).getBinding().loadingOverlay.visibility = View.VISIBLE
                SharedPrefer.updateContext(requireContext())
                val userId = SharedPrefer.getId()
                Log.d("CheckShared", userId)
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