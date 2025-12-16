package com.example.easymedia.ui.component.updateinformation

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.easymedia.R
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.data.data_source.firebase.FirebaseAuthService
import com.example.easymedia.data.repository.AuthRepository
import com.example.easymedia.data.repository.AuthRepositoryImpl
import com.example.easymedia.databinding.FragmentUpdateInformationBinding
import com.example.easymedia.ui.component.main.MainActivity
import com.example.easymedia.ui.utils.IntentExtras
import com.example.easymedia.ui.utils.SharedPrefer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class UpdateInformationFragment : Fragment() {
    private var name: String? = ""
    private var gender: String? = "Other"
    private var avatar: String? = ""
    private var introduce: String? = ""
    private var address: String? = ""
    private lateinit var binding: FragmentUpdateInformationBinding
    private var selectedImageUri: Uri? = null
    private val repoAuth: AuthRepository =
        AuthRepositoryImpl(FirebaseAuthService(CloudinaryServiceImpl()))

    override fun onResume() {
        super.onResume()
        requireActivity().window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            name = it.getString(IntentExtras.EXTRA_NAME, "")
            gender = it.getString(IntentExtras.EXTRA_GENDER)
            avatar = it.getString(IntentExtras.EXTRA_AVATAR, "")
            introduce = it.getString(IntentExtras.EXTRA_INTRODUCE, "")
            address = it.getString(IntentExtras.EXTRA_ADDRESS, "")
        }

        binding = FragmentUpdateInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as MainActivity).hideLoading()

        // Hiển thị thông tin lên trên giao diện
        binding.etName.append(name)
        binding.etIntroduce.append(introduce)
        binding.etAddress.append(address)

        // Hiển thị ảnh người dùng
        if (avatar?.isEmpty() == true) {
            binding.ivAvatar.setImageResource(R.drawable.ic_avatar)
        } else Glide.with(this).load(avatar).error(R.drawable.ic_avatar).into(binding.ivAvatar)

        val items = listOf("Nam", "Nữ", "Khác")

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGender.adapter = adapter

        if (gender.equals("Khác")) {
            binding.spinnerGender.setSelection(2)
        } else if (gender.equals("Nam")) {
            binding.spinnerGender.setSelection(0)
        } else binding.spinnerGender.setSelection(1)

        // khi mà chọn ảnh
        val pickMedia =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                uri?.let {
                    selectedImageUri = it
                    binding.ivAvatar.setImageURI(it)
                }
            }

        binding.btnEditImage.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.ivBackArrow.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Sau khi mà thay đổi thông tin thì cần phải quay trở lại màn hình Home
        // và cập nhật thông tin nằm bên trong SharedPrefernces
        binding.btSaveChanges.setOnClickListener {
            (activity as MainActivity).showLoading()
            val avatarFile = selectedImageUri?.let {
                uriToFile(
                    requireContext(),
                    it
                )
            }
            val userId = SharedPrefer.getId()
            CoroutineScope(Dispatchers.IO).launch {
                val result = repoAuth.updateUserProfile(
                    userId,
                    binding.etName.text.toString().trim(),
                    binding.etIntroduce.text.toString().trim(),
                    binding.etAddress.text.toString().trim(),
                    avatarFile,
                    binding.spinnerGender.selectedItem.toString().trim()
                )

                result.onSuccess {
                    // thành công
                    val result = repoAuth.getUserById(userId)
                    result.onSuccess { user ->
                        SharedPrefer.saveAllData(
                            userId,
                            user?.username,
                            user?.fullName,
                            user?.email,
                            user?.bio,
                            user?.location,
                            user?.gender,
                            user?.profilePicture
                        )
                        withContext(Dispatchers.Main) {
                            (activity as MainActivity).hideLoading()
                            (activity as MainActivity).clearBackStackExceptFirst()
                            parentFragmentManager.setFragmentResult(
                                "request_post_added",
                                bundleOf("isAdded" to true)
                            )
                            parentFragmentManager.popBackStack()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as MainActivity).showBottomBar()
    }

    override fun onPause() {
        super.onPause()
        requireActivity().window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
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

