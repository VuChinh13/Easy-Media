package com.example.easymedia.ui.component.story

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.palette.graphics.Palette
import com.antonkarpenko.ffmpegkit.FFmpegKit
import com.antonkarpenko.ffmpegkit.ReturnCode
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.easymedia.R
import com.example.easymedia.data.model.Music
import com.example.easymedia.data.model.VideoEditState
import com.example.easymedia.databinding.ActivityStoryBinding
import com.example.easymedia.ui.component.music.MusicBottomSheet
import com.example.easymedia.ui.component.utils.IntentExtras
import gun0912.tedimagepicker.builder.TedImagePicker
import gun0912.tedimagepicker.builder.type.MediaType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class StoryActivity : AppCompatActivity() {
    private lateinit var player: ExoPlayer
    private val storyViewModel: StoryViewModel by viewModels()
    private val overlayInfo = TextOverlayInfo()
    private var success = false
    private lateinit var binding: ActivityStoryBinding
    private lateinit var musicPlayer: ExoPlayer   // Player riêng cho nhạc nền (optional)
    private var videoEditState = VideoEditState() // trạng thái mặc định
    private var selectedUri: Uri? = null
    private var isMuted = false
    private var textStyleSelected = "lato"
    private var musicSelected: Music? = null
    private val bottomSheet = MusicBottomSheet { music ->
        finishChooseMusic(music)
    }
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector
    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()

        // ----- Click listener ngắn gọn (gọi từ UI) -----
        binding.btnSharedStory.setOnClickListener {
            binding.btnSharedStory.visibility = View.GONE
            if (::musicPlayer.isInitialized) {
                musicPlayer.stop()
                musicPlayer.release()
            }
            // binding.loading.visibility = View.VISIBLE
            renderVideoWithOverlay(this, selectedUri!!, binding.blockImage)
        }

//            binding.loading.visibility = View.VISIBLE
//            // Tạo đối tượng Story
//            val userId = SharedPrefer.getId()
//            val story = Story(userId = userId, music = musicSelected)
//            binding.blockImage.post {
//                val bitmap = captureBlockImage()
//                if (bitmap != null) {
//                    storyViewModel.uploadStory(story, bitmapToFile(this, bitmap))
//                }
//            }

        storyViewModel.finish.observe(this) {
            if (it) {
                binding.loading.visibility = View.GONE
                val resultIntent = intent
                success = true
                resultIntent.putExtra(IntentExtras.RESULT_DATA, true)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
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

        scaleGestureDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    scaleFactor *= detector.scaleFactor
                    scaleFactor = scaleFactor.coerceIn(0.5f, 5f) // giới hạn phóng to/thu nhỏ
                    applyMatrix()
                    return true
                }
            })

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                translateX -= distanceX
                translateY -= distanceY
                applyMatrix()
                return true
            }
        })

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

                    MotionEvent.ACTION_UP -> {
                        // Lưu lại vị trí cuối cùng
                        overlayInfo.posX = view.x
                        overlayInfo.posY = view.y
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

        binding.videoTexture.setOnTouchListener { v, event ->
            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            true
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
        binding.videoTexture.visibility = View.GONE
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
    /** Hiển thị video với ExoPlayer, giữ gradient, dimensionRatio, loop, mute **/
    private fun showVideoPreview(uri: Uri) {
        binding.videoTexture.visibility = View.VISIBLE
        binding.imagePreview.visibility = View.GONE
        binding.btnSound.visibility = View.VISIBLE

        // 1️⃣ Lấy frame đầu tiên → gradient background
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, uri)
        retriever.getFrameAtTime(0)?.let { bitmap ->
            Palette.from(bitmap).generate { palette ->
                val dominantColor = palette?.getDominantColor(Color.DKGRAY) ?: Color.DKGRAY
                val gradient = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(dominantColor, Color.BLACK)
                )
                binding.blockImage.background = gradient
            }
        }
        retriever.release()

        // 2️⃣ Khởi tạo ExoPlayer
        player = ExoPlayer.Builder(this).build()
        player.setMediaItem(MediaItem.fromUri(uri))
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.playWhenReady = true

        // 3️⃣ Gắn TextureView
        player.setVideoTextureView(binding.videoTexture)

        // 4️⃣ Lắng nghe VideoSize → set dimensionRatio
        player.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                val params = binding.videoTexture.layoutParams as ConstraintLayout.LayoutParams
                params.dimensionRatio = "${videoSize.width}:${videoSize.height}"
                binding.videoTexture.layoutParams = params
            }

            override fun onRenderedFirstFrame() {
                // Sound mặc định ON
                player.volume = 1f
                isMuted = false
                binding.btnSound.setImageResource(R.drawable.ic_sound)
            }
        })

        player.prepare()
    }

    /** Bật / tắt âm thanh video **/
    private fun toggleSound() {
        isMuted = !isMuted
        player.volume = if (isMuted) 0f else 1f
        binding.btnSound.setImageResource(
            if (isMuted) R.drawable.ic_sound_off else R.drawable.ic_sound
        )
        videoEditState = videoEditState.copy(removeOriginalAudio = isMuted)
        Log.d("StoryActivity", "Âm thanh: ${if (isMuted) "TẮT" else "BẬT"}")
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

    private fun applyMatrix() {
        val matrix = android.graphics.Matrix()
        matrix.postScale(
            scaleFactor,
            scaleFactor,
            binding.videoTexture.width / 2f,
            binding.videoTexture.height / 2f
        )
        matrix.postTranslate(translateX, translateY)
        binding.videoTexture.setTransform(matrix)
    }

    /** Chọn nhạc nền → tạo ExoPlayer riêng cho nhạc **/
    private fun finishChooseMusic(music: Music?) {
        musicSelected = music
        musicPlayer.release()

        music?.url?.let { url ->
            musicPlayer = ExoPlayer.Builder(this).build().apply {
                setMediaItem(MediaItem.fromUri(url))
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = true
                volume = 1f
                prepare()
            }
        }

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

    fun renderVideoWithOverlay(context: Context, videoUri: Uri, overlayView: View) {

        val inputVideo = copyVideoToCache(context, videoUri)

        // 1. Chụp toàn bộ blockImage
        val overlayBitmap = createOverlayBitmap(overlayView)

        // 2. Lấy kích thước video thật
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(inputVideo.absolutePath)
        val videoW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
        val videoH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
        retriever.release()

        // 3. Scale overlay = cùng kích thước video
        val scaledOverlay = Bitmap.createScaledBitmap(overlayBitmap, videoW, videoH, true)

        // 4. Lưu overlay PNG
        val overlayFile = saveOverlayToFile(context, scaledOverlay)

        val outputTemp = File(context.cacheDir, "output_render.mp4")

        // 5. Ghép overlay lên video
        val cmd = arrayOf(
            "-y",
            "-i", inputVideo.absolutePath,
            "-i", overlayFile.absolutePath,
            "-filter_complex", "overlay=0:0",
            "-c:v", "libx264",
            "-preset", "ultrafast",
            "-crf", "20",
            "-c:a", "aac",
            outputTemp.absolutePath
        )

        FFmpegKit.executeAsync(cmd.joinToString(" ")) { session ->
            if (ReturnCode.isSuccess(session.returnCode)) {

                val finalUri = saveVideoToPublic(context, outputTemp)
                Log.d("FFmpeg", "Saved to public: $finalUri")

            } else {
                Log.e("FFmpeg", "Error: ${session.returnCode}")
                Log.e("FFmpeg", session.allLogsAsString)
            }
        }
    }

    fun copyVideoToCache(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)!!
        val outputFile = File(context.cacheDir, "input_video.mp4")

        FileOutputStream(outputFile).use { out ->
            inputStream.copyTo(out)
        }

        return outputFile
    }

    fun saveVideoToPublic(context: Context, source: File): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/EasyMedia/")
            put(MediaStore.Video.Media.DISPLAY_NAME, "story_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)!!

        resolver.openOutputStream(uri).use { out ->
            FileInputStream(source).copyTo(out!!)
        }

        // Mark as finished
        contentValues.clear()
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)

        return uri
    }

    fun createOverlayBitmap(view: View): Bitmap {
        // Đảm bảo view đã được layout
        if (view.width == 0 || view.height == 0) {
            throw IllegalStateException("View has no size! Call this AFTER view is laid out.")
        }

        val bitmap = Bitmap.createBitmap(
            view.width,
            view.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        view.draw(canvas)

        return bitmap
    }


    fun saveOverlayToFile(context: Context, bmp: Bitmap): File {
        val file = File(context.filesDir, "overlay.png")
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::musicPlayer.isInitialized) {
            musicPlayer.stop()
            musicPlayer.release()
        }

        if (::player.isInitialized) {
            player.stop()
            player.release()
        }
    }
}

data class TextOverlayInfo(
    var text: String = "",
    var posX: Float = 0f,
    var posY: Float = 0f,
    var textSize: Float = 22f,
    var textColor: String = "white", // FFmpeg dùng màu string
    var fontPath: String = ""        // nếu cần custom font
)
