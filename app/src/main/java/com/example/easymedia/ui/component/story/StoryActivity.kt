package com.example.easymedia.ui.component.story

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.MediaController
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.easymedia.R
import com.example.easymedia.data.model.Music
import com.example.easymedia.data.model.Story
import com.example.easymedia.data.model.VideoEditState
import com.example.easymedia.databinding.ActivityStoryBinding
import com.example.easymedia.ui.component.music.MusicBottomSheet
import com.example.easymedia.ui.component.utils.IntentExtras
import com.example.easymedia.ui.component.utils.SharedPrefer
import com.example.easymedia.ui.component.utils.SharedPrefer.context
import gun0912.tedimagepicker.builder.TedImagePicker
import gun0912.tedimagepicker.builder.type.MediaType
import java.io.File
import java.io.FileOutputStream

class StoryActivity : AppCompatActivity() {
    private val storyViewModel: StoryViewModel by viewModels()
    private var success = false
    private lateinit var binding: ActivityStoryBinding
    private var overlayTextView: TextView? = null
    private var videoEditState = VideoEditState() // trạng thái mặc định
    private var selectedUri: Uri? = null
    private var isMuted = false
    private var mediaPlayer: MediaPlayer? = null
    private var textStyleSelected = "lato"
    private var musicSelected: Music? = null
    private val bottomSheet = MusicBottomSheet { music ->
        finishChooseMusic(music)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()

        binding.btnSharedStory.setOnClickListener {
            binding.btnSharedStory.visibility = View.GONE
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            binding.loading.visibility = View.VISIBLE
            // Tạo đối tượng Story
            val userId = SharedPrefer.getId()
            val story = Story(userId = userId, music = musicSelected)
            binding.blockImage.post {
                val bitmap = captureBlockImage()
                if (bitmap != null) {
                    storyViewModel.uploadStory(story, bitmapToFile(this, bitmap))
                }
            }
        }

        storyViewModel.finish.observe(this) {
            if (it) {
                binding.loading.visibility = View.GONE
                val resultIntent = intent
                success = true
                resultIntent.putExtra(IntentExtras.RESULT_DATA, success)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    // hàm này dùng để mà lưu ảnh vào bên trong máy
    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val filename = "story_${System.currentTimeMillis()}.png"

        // Tạo thông tin file để MediaStore quản lý
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AppMedia") // thư mục trong Gallery
            put(MediaStore.Images.Media.IS_PENDING, 1) // tạm thời để ghi xong mới hiển thị
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let { imageUri ->
            resolver.openOutputStream(imageUri)?.use { outStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            }

            // Ghi xong, đánh dấu ảnh hoàn tất để hiển thị trong Gallery
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(imageUri, values, null, null)
        }
    }

    private fun captureBlockImage(): Bitmap? {
        val view = binding.blockImage

        // Kiểm tra view đã có kích thước
        if (view.width == 0 || view.height == 0) return null

        // Tạo bitmap cùng kích thước với view
        val bitmap = createBitmap(view.width, view.height)

        // Tạo canvas từ bitmap
        val canvas = Canvas(bitmap)

        // Vẽ view lên canvas (bao gồm tất cả view con bên trong)
        view.draw(canvas)

        return bitmap
    }

    fun bitmapToFile(context: Context, bitmap: Bitmap): File {
        // Tạo file tạm trong thư mục cache
        val file = File(context.cacheDir, "story_${System.currentTimeMillis()}.png")

        // Ghi bitmap ra file
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return file
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI() {
        // Mở trình chọn ảnh/video
        TedImagePicker.with(this)
            .title("Thêm vào tin")
            .showCameraTile(true)
            .mediaType(MediaType.IMAGE_AND_VIDEO)
            .cancelListener {
                finish()
            }
            .start { uri ->
                Log.d("StoryActivity", "Đã chọn: $uri")
                selectedUri = uri
                showPreview(uri)
            }

        binding.btnSound.setOnClickListener {
            toggleSound()
        }

        binding.btnAddText.setOnClickListener {
            hideAddText()
        }

        binding.etEditableText.setOnTouchListener(object : View.OnTouchListener {
            private var dX = 0f
            private var dY = 0f

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dX = view.x - event.rawX
                        dY = view.y - event.rawY
                    }

                    MotionEvent.ACTION_MOVE -> {
                        view.animate()
                            .x(event.rawX + dX)
                            .y(event.rawY + dY)
                            .setDuration(0)
                            .start()
                    }
                }
                return true
            }
        })

        binding.blockMusic.setOnTouchListener(object : View.OnTouchListener {
            private var dX = 0f
            private var dY = 0f

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dX = view.x - event.rawX
                        dY = view.y - event.rawY
                    }

                    MotionEvent.ACTION_MOVE -> {
                        view.animate()
                            .x(event.rawX + dX)
                            .y(event.rawY + dY)
                            .setDuration(0)
                            .start()
                    }
                }
                return true
            }
        })

        binding.imagePreview.minimumScale = 0.5f // cho phép nhỏ bằng 50% khung
        binding.imagePreview.maximumScale = 5f   // cho phép phóng to 5x

        // sự kiện chọn màu
        binding.btnTextLobster.setOnClickListener {
            changeBackgroundText2(textStyleSelected)
            textStyleSelected = "lobster"
            changeBackgroundText(textStyleSelected)
        }

        // sự kiện chọn màu
        binding.btnTextLato.setOnClickListener {
            changeBackgroundText2(textStyleSelected)
            textStyleSelected = "lato"
            changeBackgroundText(textStyleSelected)
        }

        // sự kiện chọn màu
        binding.btnTextTiltNeon.setOnClickListener {
            changeBackgroundText2(textStyleSelected)
            textStyleSelected = "tiltneon"
            changeBackgroundText(textStyleSelected)
        }

        // sự kiện chọn màu
        binding.btnTextYesteryear.setOnClickListener {
            changeBackgroundText2(textStyleSelected)
            textStyleSelected = "yesteryear"
            changeBackgroundText(textStyleSelected)
        }

        binding.btnColor.setOnClickListener {
            binding.blockText.visibility = View.GONE
            binding.blockColor.visibility = View.VISIBLE
        }

        binding.btnTextStyle.setOnClickListener {
            binding.blockColor.visibility = View.GONE
            binding.blockText.visibility = View.VISIBLE
        }

        changeTextColor()

        binding.btnFinish.setOnClickListener {
            finishAddText()
        }

        // sự kiện chọn nhạc
        binding.btnMusic.setOnClickListener {
            bottomSheet.show(supportFragmentManager, null)
            storyViewModel.getAllMusic()
        }

        // Lấy được danh sách nhạc
        storyViewModel.listMusic.observe(this) { listMusic ->
            bottomSheet.updateListMusic(listMusic.toMutableList())
        }

        binding.btnClose.setOnClickListener { finish() }
    }

    /** Hiển thị preview ảnh hoặc video **/
    private fun showPreview(uri: Uri) {
        val contentType = contentResolver.getType(uri)

        if (contentType?.startsWith("image/") == true) {
            showImagePreview(uri)
        } else if (contentType?.startsWith("video/") == true) {
            showVideoPreview(uri)
        }
    }

    /** Xử lý hiển thị ảnh với tỉ lệ thật **/
    private fun showImagePreview(uri: Uri) {
        binding.imagePreview.visibility = View.VISIBLE
        binding.videoPreview.visibility = View.GONE
        binding.btnSound.visibility = View.INVISIBLE

        Glide.with(this)
            .asBitmap()
            .load(uri)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    // 1️⃣ Tạo gradient nền
                    Palette.from(resource).generate { palette ->
                        val dominantColor = palette?.getDominantColor(Color.DKGRAY) ?: Color.DKGRAY
                        val gradient = GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(dominantColor, Color.BLACK)
                        )
                        binding.blockImage.background = gradient
                    }

                    // 2️⃣ Tính tỷ lệ chiều cao ảnh
                    val width = resource.width
                    val height = resource.height
                    val screenWidth = binding.imagePreview.width.takeIf { it > 0 }
                        ?: resources.displayMetrics.widthPixels
                    val newHeight = (screenWidth.toFloat() / width * height).toInt()

                    val params = binding.imagePreview.layoutParams
                    params.height = newHeight
                    binding.imagePreview.layoutParams = params

                    // 3️⃣ Hiển thị ảnh
                    binding.imagePreview.setImageBitmap(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    /** Xử lý hiển thị video với tỉ lệ thật **/
    private fun showVideoPreview(uri: Uri) {
        binding.imagePreview.visibility = View.GONE
        binding.videoPreview.visibility = View.VISIBLE

        binding.videoPreview.setVideoURI(uri)
        val mediaController = MediaController(this)
        mediaController.setAnchorView(binding.videoPreview)
        binding.videoPreview.setMediaController(mediaController)

        binding.videoPreview.setOnPreparedListener { mp ->
            val videoWidth = mp.videoWidth
            val videoHeight = mp.videoHeight
            val ratio = "$videoWidth:$videoHeight"

            val params = binding.videoPreview.layoutParams as ConstraintLayout.LayoutParams
            params.dimensionRatio = "H,$ratio"
            binding.videoPreview.layoutParams = params

            mp.isLooping = true
            binding.videoPreview.start()

            mp.setVolume(1f, 1f) // bật tiếng mặc định
            isMuted = false
            binding.btnSound.setImageResource(R.drawable.ic_sound) // icon âm thanh
        }
    }

    /** Bật / tắt âm thanh video **/
    @SuppressLint("DiscouragedPrivateApi")
    private fun toggleSound() {
        val videoView = binding.videoPreview
        if (videoView.visibility != View.VISIBLE) return

        isMuted = !isMuted

        try {
            val mediaPlayerField =
                android.widget.VideoView::class.java.getDeclaredField("mMediaPlayer")
            mediaPlayerField.isAccessible = true
            val mediaPlayer = mediaPlayerField.get(videoView) as? MediaPlayer

            mediaPlayer?.setVolume(
                if (isMuted) 0f else 1f,
                if (isMuted) 0f else 1f
            )

            // 🔹 Cập nhật vào trạng thái chỉnh sửa
            videoEditState = videoEditState.copy(removeOriginalAudio = isMuted)

            Log.d(
                "StoryActivity",
                "Âm thanh: ${if (isMuted) "TẮT" else "BẬT"} — removeOriginalAudio = ${videoEditState.removeOriginalAudio}"
            )

            // Đổi icon cho nút âm thanh
            binding.btnSound.setImageResource(
                if (isMuted) R.drawable.ic_sound_off else R.drawable.ic_sound
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Xóa lựa chọn của mình
    private fun clearSelection() {
        selectedUri = null
        binding.imagePreview.visibility = View.GONE
        binding.videoPreview.visibility = View.GONE
        binding.videoPreview.stopPlayback()

        overlayTextView?.let { binding.main.removeView(it) }
        overlayTextView = null

        videoEditState = VideoEditState() // reset trạng thái
    }


    @SuppressLint("ServiceCast")
    private fun hideAddText() {
        // Ẩn các nút khác
        binding.btnClose.visibility = View.INVISIBLE
        binding.btnAddText.visibility = View.INVISIBLE
        binding.btnSound.visibility = View.INVISIBLE
        binding.btnMusic.visibility = View.INVISIBLE
        binding.btnMore.visibility = View.INVISIBLE
        binding.btnFinish.visibility = View.VISIBLE
        binding.blockButton.visibility = View.VISIBLE

        // Hiển thị EditText và focus vào đó
        binding.etEditableText.visibility = View.VISIBLE
        // đảm bảo cursor hiện và chọn hết text nếu có
        binding.etEditableText.isCursorVisible = true
        binding.etEditableText.setSelection(binding.etEditableText.text!!.length)
        binding.etEditableText.requestFocus()

        // Mở bàn phím
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etEditableText, InputMethodManager.SHOW_IMPLICIT)

//        binding.blockColor.visibility = View.VISIBLE
        binding.blockText.visibility = View.VISIBLE
    }

    @SuppressLint("ServiceCast")
    private fun finishAddText() {
        binding.btnClose.visibility = View.VISIBLE
        binding.btnAddText.visibility = View.VISIBLE
        binding.btnMusic.visibility = View.VISIBLE
        binding.btnMore.visibility = View.VISIBLE
        binding.blockButton.visibility = View.GONE
        binding.blockText.visibility = View.GONE
        binding.blockColor.visibility = View.GONE
        binding.btnFinish.visibility = View.GONE
        binding.etEditableText.isCursorVisible = false
        binding.etEditableText.clearFocus()
        // Ẩn bàn phím
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etEditableText.windowToken, 0)
    }

    private fun finishChooseMusic(music: Music?) {
        // thực hiện cái gì đó luôn á
        musicSelected = music
        playLoopingMusic(music)
        binding.blockMusic.visibility = View.VISIBLE
        binding.tvArtist.text = music?.artist
        binding.tvTitle.text = music?.title
    }

    private fun changeBackgroundText(textStyle: String) {
        when (textStyle) {
            "lato" -> {
                binding.btnTextLato.setBackgroundResource(R.drawable.bg_text_style_selection) // bg_new.xml
                binding.btnTextLato.setTextColor(Color.BLACK)
                val typeface = ResourcesCompat.getFont(this, R.font.lato_bold)
                binding.etEditableText.typeface = typeface
            }

            "lobster" -> {
                binding.btnTextLobster.setBackgroundResource(R.drawable.bg_text_style_selection) // bg_new.xml
                binding.btnTextLobster.setTextColor(Color.BLACK)
                val typeface = ResourcesCompat.getFont(this, R.font.lobster_regular_font)
                binding.etEditableText.typeface = typeface
            }

            "tiltneon" -> {
                binding.btnTextTiltNeon.setBackgroundResource(R.drawable.bg_text_style_selection) // bg_new.xml
                binding.btnTextTiltNeon.setTextColor(Color.BLACK)
                val typeface = ResourcesCompat.getFont(this, R.font.tiltneon_regular)
                binding.etEditableText.typeface = typeface
            }

            "yesteryear" -> {
                binding.btnTextYesteryear.setBackgroundResource(R.drawable.bg_text_style_selection) // bg_new.xml
                binding.btnTextYesteryear.setTextColor(Color.BLACK)
                val typeface = ResourcesCompat.getFont(this, R.font.yesteryear_regular)
                binding.etEditableText.typeface = typeface
            }
        }
    }

    private fun changeBackgroundText2(textStyle: String) {
        when (textStyle) {
            "lato" -> {
                binding.btnTextLato.setBackgroundResource(R.drawable.bg_text_style) // bg_new.xml
                binding.btnTextLato.setTextColor(Color.WHITE)
            }

            "lobster" -> {
                binding.btnTextLobster.setBackgroundResource(R.drawable.bg_text_style) // bg_new.xml
                binding.btnTextLobster.setTextColor(Color.WHITE)
            }

            "tiltneon" -> {
                binding.btnTextTiltNeon.setBackgroundResource(R.drawable.bg_text_style) // bg_new.xml
                binding.btnTextTiltNeon.setTextColor(Color.WHITE)
            }

            "yesteryear" -> {
                binding.btnTextYesteryear.setBackgroundResource(R.drawable.bg_text_style) // bg_new.xml
                binding.btnTextYesteryear.setTextColor(Color.WHITE)
            }
        }
    }

    private fun changeTextColor() {
        binding.btnColorWhite.setOnClickListener {
            binding.etEditableText.setBackgroundResource(R.drawable.bg_block_button) // bg_new.xml
            binding.etEditableText.setTextColor(Color.WHITE)
        }
        binding.btnColorBlack.setOnClickListener {
            binding.etEditableText.setBackgroundResource(R.drawable.bg_block_button2) // bg_new.xml
            binding.etEditableText.setTextColor(Color.BLACK)
        }
        binding.btnColorOrange.setOnClickListener {
            binding.etEditableText.setBackgroundResource(R.drawable.bg_block_button2) // bg_new.xml
            binding.etEditableText.setTextColor("#e65100".toColorInt())
        }
        binding.btnColorBlue.setOnClickListener {
            binding.etEditableText.setBackgroundResource(R.drawable.bg_block_button2) // bg_new.xml
            binding.etEditableText.setTextColor("#0d47a1".toColorInt())
        }
        binding.btnColorGrey.setOnClickListener {
            binding.etEditableText.setBackgroundResource(R.drawable.bg_block_button2) // bg_new.xml
            binding.etEditableText.setTextColor("#757575".toColorInt())
        }
        binding.btnColorGreen.setOnClickListener {
            binding.etEditableText.setBackgroundResource(R.drawable.bg_block_button2) // bg_new.xml
            binding.etEditableText.setTextColor("#2e7d32".toColorInt())
        }
        binding.btnColorRed.setOnClickListener {
            binding.etEditableText.setBackgroundResource(R.drawable.bg_block_button2) // bg_new.xml
            binding.etEditableText.setTextColor("#b71c1c".toColorInt())
        }
        binding.btnColorYellow.setOnClickListener {
            binding.etEditableText.setBackgroundResource(R.drawable.bg_block_button) // bg_new.xml
            binding.etEditableText.setTextColor("#ffff00".toColorInt())
        }
    }

    private fun playLoopingMusic(music: Music?) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        mediaPlayer = MediaPlayer().apply {
            setDataSource(music?.url)
            isLooping = true // 🔁 Phát lặp lại vô hạn
            setOnPreparedListener { start() }
            setOnErrorListener { _, _, _ -> false }
            prepareAsync()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
