package com.example.instagram.ui.component.addpost

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.instagram.databinding.FragmentAddPostBinding
import com.example.instagram.ui.component.main.MainActivity
import com.example.instagram.ui.component.utils.AppToast
import com.example.instagram.ui.component.utils.SharedPrefer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

        addPostViewModel.getPostResponse.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                // Nếu mà thành công
                Toast.makeText(
                    requireContext(),
                    "Đã chia sẻ thành công bài viết",
                    Toast.LENGTH_SHORT
                )
                    .show()
                parentFragmentManager.popBackStack()
            } else Toast.makeText(
                requireContext(),
                "Đã có lỗi xảy ra hãy kiểm tra lại",
                Toast.LENGTH_SHORT
            )
                .show()
        }


        binding.btShare.setOnClickListener {
            if (binding.etContent.text.isEmpty()) {
                AppToast.show(requireContext(),"Hãy nhập nội dung bài viết")
            } else {
                SharedPrefer.updateContext(requireContext())
                val userId = SharedPrefer.getUserId()
                addPostViewModel.addPost(userId, binding.etContent.text.toString(), postImages)
            }
        }

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