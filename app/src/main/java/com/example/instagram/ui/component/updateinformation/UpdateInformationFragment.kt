//package com.example.instagram.ui.component.updateinformation
//
//import android.content.Context
//import android.net.Uri
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ArrayAdapter
//import android.widget.Toast
//import androidx.activity.result.PickVisualMediaRequest
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.viewModels
//import com.bumptech.glide.Glide
//import com.example.instagram.R
//import com.example.instagram.databinding.FragmentUpdateInformationBinding
//import com.example.instagram.ui.component.utils.IntentExtras
//import java.io.File
//import java.io.FileOutputStream
//
//class UpdateInformationFragment : Fragment() {
//    private var name: String? = ""
//    private var gender: String? = "Other"
//    private var avatar: String? = ""
//    private var introduce: String? = ""
//    private var address: String? = ""
//    private lateinit var binding: FragmentUpdateInformationBinding
//    private var selectedImageUri: Uri? = null
//    private val updateInformationViewModel: UpdateInformationViewModel by viewModels()
//
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        arguments?.let {
//            name = it.getString(IntentExtras.EXTRA_NAME)
//            gender = it.getString(IntentExtras.EXTRA_GENDER)
//            avatar = it.getString(IntentExtras.EXTRA_AVATAR)
//            introduce = it.getString(IntentExtras.EXTRA_INTRODUCE)
//            address = it.getString(IntentExtras.EXTRA_ADDRESS)
//        }
//
//        binding = FragmentUpdateInformationBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        binding.etName.append(name)
//        binding.etIntroduce.append(introduce)
//        binding.etAddress.append(address)
//
//        if (avatar?.isEmpty() == true) {
//            binding.ivAvatar.setImageResource(R.drawable.ic_avatar)
//        } else Glide.with(this).load(avatar).error(R.drawable.ic_avatar).into(binding.ivAvatar)
//        val items = listOf("Nam", "Nữ", "Khác")
//        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        binding.spinnerGender.adapter = adapter
//        if (gender.equals("Khác")) {
//            binding.spinnerGender.setSelection(2)
//        } else if (gender.equals("Nam")) {
//            binding.spinnerGender.setSelection(0)
//
//        } else binding.spinnerGender.setSelection(1)
//        val sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
//        val userId = sharedPreferences.getString("_id", "") ?: ""
//
//        updateInformationViewModel.updateInforResult.observe(viewLifecycleOwner) { result ->
//            if (result != null) {
//                val sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
//                val editor = sharedPreferences.edit()
//                editor.putString("username", result.data.username)
//                editor.putString("password", result.data.password)
//                editor.putString("name", result.data.name)
//                editor.putString("avatar", result.data.avatar)
//                editor.putString("gender", result.data.gender)
//                editor.putString("address", result.data.address)
//                editor.putString("introduce", result.data.introduce)
//                Toast.makeText(
//                    requireActivity(),
//                    "Updated information successfully",
//                    Toast.LENGTH_SHORT
//                ).show()
//            } else {
//                Toast.makeText(
//                    requireActivity(),
//                    " Error, please check the information again!",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//
//        val pickMedia =
//            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
//                uri?.let {
//                    selectedImageUri = it
//                    binding.ivAvatar.setImageURI(it)
//                }
//            }
//        binding.ivAvatar.setOnClickListener {
//            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
//        }
//        binding.ivBackArrow.setOnClickListener {
//            parentFragmentManager.popBackStack()
//        }
//        binding.btSaveChanges.setOnClickListener {
//            val avatarFile = selectedImageUri?.let {
//                uriToFile(
//                    requireContext(),
//                    it
//                )
//            }
//            if (binding.etOldPassword.text.toString()
//                    .isEmpty() && binding.etNewPassword.text.toString().isEmpty()
//            ) {
//                updateInformationViewModel.updateInformation(
//                    null,
//                    null,
//                    binding.etName.text.toString().trim(),
//                    avatarFile,
//                    binding.spinnerGender.selectedItem.toString().trim(),
//                    binding.etAddress.text.toString().trim(),
//                    binding.etIntroduce.text.toString().trim(),
//                    userId
//                )
//            } else {
//                updateInformationViewModel.updateInformation(
//                    binding.etOldPassword.text.toString().trim(),
//                    binding.etNewPassword.text.toString().trim(),
//                    binding.etName.text.toString().trim(),
//                    avatarFile,
//                    binding.spinnerGender.selectedItem.toString().trim(),
//                    binding.etAddress.text.toString().trim(),
//                    binding.etIntroduce.text.toString().trim(),
//                    userId
//                )
//            }
//        }
//
//    }
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
//
//}
