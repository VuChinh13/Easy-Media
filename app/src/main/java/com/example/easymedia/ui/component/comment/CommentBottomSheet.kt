package com.example.easymedia.ui.component.comment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.data.data_source.firebase.FirebasePostService
import com.example.easymedia.data.model.Comment
import com.example.easymedia.data.repository.PostRepositoryImpl
import com.example.easymedia.databinding.CommentBottomsheetBinding
import com.example.easymedia.ui.component.comment.adapter.CommentAdapter
import com.example.easymedia.ui.component.home.OnAvatarClickListener
import com.example.easymedia.ui.utils.SharedPrefer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.isNotEmpty

// bên trong lớp này thì nhận vào đó là danh sách các comment
class CommentBottomSheet(
    private val postId: String,
    private val userId: String,
    private val position: Int,
    private var totalComment: Int,
    private val onCommentChanged: (position: Int, totalComment: Int) -> Unit,
    private val listener: OnAvatarClickListener?
) : BottomSheetDialogFragment() {

    private lateinit var binding: CommentBottomsheetBinding
    private lateinit var adapter: CommentAdapter
    private val repositoryPost =
        PostRepositoryImpl(FirebasePostService(cloudinary = CloudinaryServiceImpl()))
    private var listComment = mutableListOf<Comment>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = CommentBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        // Làm BottomSheet cao full màn hình
        val dialog = dialog as? BottomSheetDialog
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                val layoutParams = sheet.layoutParams
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                sheet.layoutParams = layoutParams
                behavior.state = BottomSheetBehavior.STATE_EXPANDED // Mở full ngay khi show
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Đây là nơi mà thực hiện Logic

        val profilePicture = SharedPrefer.getProfilePicture()
        Glide.with(view)
            .load(profilePicture)
            .error(com.example.easymedia.R.drawable.ic_avatar)
            .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
            .into(binding.ivAvatar)

        // Khởi tạo Adapter
        adapter = CommentAdapter(
            listComment, postId, {
                reloadComments()
            }, {
                // ⬅️ TRUYỀN DISMISS VÀO ĐÂY
                dismiss()            // <<< BottomSheet tự đóng
            }, listener
        )
        binding.rvComments.layoutManager = LinearLayoutManager(context)
        binding.rvComments.adapter = adapter


        CoroutineScope(Dispatchers.IO).launch {
            val result = repositoryPost.getComments(postId)
            result.onSuccess { list ->
                listComment.addAll(list)
                withContext(Dispatchers.Main) {
                    // Kiểm tra xem là danh sách mà truyền vào bên trong có rỗng hay không
                    if (listComment.isNotEmpty()) {
                        hideEmptyComment()

                        adapter.notifyDataSetChanged()
                    } else {
                        // Nếu mà trống thì làm gì đó
                        showEmptyComment()
                    }
                }
            }.onFailure {
                // khi mà lỗi thì làm gì đó
            }
        }

        // Sự kiện khi mà nhấn đăng comment
        binding.etComment.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // Gọi trước khi mà thay đổi Text
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                // Gọi khi mà đang thay đổi Text
                if (!s.isNullOrEmpty()) {
                    binding.btnUpload.visibility = View.VISIBLE
                } else {
                    binding.btnUpload.visibility = View.INVISIBLE
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Gọi sau khi mà Text thay đổi xong
            }
        })

        binding.btnUpload.setOnClickListener {
            Toast.makeText(context, "Đang tải bình luận", Toast.LENGTH_SHORT).show()
            CoroutineScope(Dispatchers.IO).launch {
                val result =
                    repositoryPost.addComment(
                        postId,
                        userId,
                        binding.etComment.text.toString()
                    )
                result.onSuccess {
                    withContext(Dispatchers.Main) {
                        // Cập nhật lại danh sách
                        CoroutineScope(Dispatchers.IO).launch {
                            val result = repositoryPost.getComments(postId)
                            result.onSuccess { list ->
                                listComment.clear()
                                listComment.addAll(list)
                                withContext(Dispatchers.Main) {
                                    // Kiểm tra xem là danh sách mà truyền vào bên trong có rỗng hay không
                                    if (listComment.isNotEmpty()) {
                                        binding.tvTitle1.visibility = View.INVISIBLE
                                        binding.tvTitle2.visibility = View.INVISIBLE
                                        totalComment++
                                        onCommentChanged(position, totalComment)
                                        adapter.notifyDataSetChanged()
                                    }
                                }
                            }.onFailure {
                                // khi mà lỗi thì làm gì đó
                            }
                        }
                    }
                }.onFailure {
                    // làm gì đó
                }
            }
            // Ẩn bàn phím
            val imm =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.etComment.windowToken, 0)

            // Xóa nội dung EditText
            binding.etComment.text?.clear()
            binding.etComment.clearFocus()
            binding.btnUpload.visibility = View.INVISIBLE
        }
    }

    // Dùng để mà xử lí tránh bị leak bộ nhớ
    override fun onDestroyView() {
        super.onDestroyView()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun reloadComments() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = repositoryPost.getComments(postId)
            result.onSuccess { list ->
                withContext(Dispatchers.Main) {
                    listComment.clear()
                    listComment.addAll(list)
                    adapter.notifyDataSetChanged()
                    // cập nhật lại số comment
                    totalComment--

                    // kiểm tra lại nếu mà số comment bằng 0 thì hiển thị Text
                    if (totalComment == 0) {
                        showEmptyComment()
                    } else hideEmptyComment()

                    // chỗ này thì cần có callback
                    onCommentChanged(position, totalComment)
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Lỗi khi tải lại bình luận", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun showEmptyComment() {
        binding.tvTitle1.visibility = View.VISIBLE
        binding.tvTitle2.visibility = View.VISIBLE
    }

    fun hideEmptyComment() {
        binding.tvTitle1.visibility = View.INVISIBLE
        binding.tvTitle2.visibility = View.INVISIBLE
    }
}