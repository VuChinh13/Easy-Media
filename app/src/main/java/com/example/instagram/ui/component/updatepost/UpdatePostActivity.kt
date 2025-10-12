//package com.example.instagram.ui.component.updatepost
//
//import android.content.Context
//import android.net.Uri
//import android.os.Bundle
//import android.widget.Toast
//import androidx.activity.result.PickVisualMediaRequest
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.activity.viewModels
//import androidx.appcompat.app.AppCompatActivity
//import com.bumptech.glide.Glide
//import com.example.instagram.databinding.ActivityUpdatePostBinding
//import com.example.instagram.ui.component.utils.IntentExtras
//import com.example.instagram.ui.component.utils.SharedPrefer
//import java.io.File
//import java.io.FileOutputStream
//
//class UpdatePostActivity : AppCompatActivity() {
//    private lateinit var binding: ActivityUpdatePostBinding
//    private val postImages: MutableList<File> = mutableListOf()
//    private val updatePostViewModel: UpdatePostViewModel by viewModels()
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityUpdatePostBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        val images: ArrayList<String> =
//            intent.getStringArrayListExtra(IntentExtras.EXTRA_POST_IMAGE) ?: arrayListOf()
//        val content: String = intent.getStringExtra(IntentExtras.EXTRA_POST_CONTENT) ?: ""
//        val postId: String = intent.getStringExtra(IntentExtras.EXTRA_POST_ID) ?: ""
//
//        binding.etContent.append(content)
//
//        if (images.isNotEmpty()) {
//            Glide.with(this).load(images[0]).into(binding.ivPostImage)
//        }
//
//        val pickMultipleMedia =
//            registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
//                if (uris.isNotEmpty()) {
//                    uris.forEach { uri ->
//                        binding.ivPostImage.setImageURI(uri)
//                        postImages.add(uriToFile(this, uri))
//                    }
//                }
//            }
//
//        binding.btChooseImage.setOnClickListener {
//            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
//        }
//
//        updatePostViewModel.getPostUpdateResponse.observe(this) { result ->
//            if (result != null) {
//                Toast.makeText(this, "Cập nhật bài viết thành công", Toast.LENGTH_SHORT).show()
//                finish()
//            } else Toast.makeText(this, "Đã xảy ra lỗi hãy kiểm tra lại", Toast.LENGTH_SHORT).show()
//        }
//        binding.btSave.setOnClickListener {
//           SharedPrefer.updateContext(this)
//            val userId = SharedPrefer.getUserId()
//            updatePostViewModel.updatePost(
//                userId,
//                postId,
//                postImages,
//                binding.etContent.text.toString()
//            )
//        }
//        binding.ivClose.setOnClickListener { finish() }
//    }
//
//    private fun uriToFile(context: Context, uri: Uri): File {
//        val contentResolver = context.contentResolver
//        val inputStream = contentResolver.openInputStream(uri)
//        val fileName = "image_${System.currentTimeMillis()}.jpg"
//        val file = File(context.cacheDir, fileName)
//        val outputStream = FileOutputStream(file)
//        inputStream?.copyTo(outputStream)
//        inputStream?.close()
//        outputStream.close()
//        return file
//    }
//}
