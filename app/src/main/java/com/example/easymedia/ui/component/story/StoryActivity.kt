package com.example.easymedia.ui.component.story

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.easymedia.R
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.data.data_source.firebase.FirebaseStoryService
import com.example.easymedia.data.model.Music
import com.example.easymedia.data.model.Story
import com.example.easymedia.data.model.VideoEditState
import com.example.easymedia.data.repository.StoryRepositoryImpl
import com.example.easymedia.databinding.ActivityStoryBinding
import com.example.easymedia.ui.component.music.MusicBottomSheet
import com.example.easymedia.ui.component.story.service.ImageRenderService
import com.example.easymedia.ui.component.story.service.VideoRenderService
import com.example.easymedia.ui.component.utils.IntentExtras
import com.example.easymedia.ui.component.utils.SharedPrefer
import gun0912.tedimagepicker.builder.TedImagePicker
import gun0912.tedimagepicker.builder.type.MediaType
import java.io.File
import java.io.FileOutputStream

class StoryActivity : AppCompatActivity() {
    private lateinit var player: ExoPlayer
    private val storyViewModel: StoryViewModel by viewModels()
    private val overlayInfo = TextOverlayInfo()
    private val overlayInfoMusic = TextOverlayInfo()
    private var success = false
    private lateinit var binding: ActivityStoryBinding
    private lateinit var musicPlayer: ExoPlayer   // Player riêng cho nhạc nền (optional)
    private var videoEditState = VideoEditState() // trạng thái mặc định
    private var selectedUri: Uri? = null
    private var isMuted = false
    private val imgService by lazy {
        ImageRenderService(
            this,
            StoryRepositoryImpl(FirebaseStoryService(cloudinary = CloudinaryServiceImpl()))
        )
    }
    private var isSelectedImage = true // mặc định là chọn ảnh đe
    private var textStyleSelected = "lato"
    private var musicSelected: Music? = null
    private var originalX = 0f
    private var originalY = 0f
    private var originalMusicX = 0f
    private var originalMusicY = 0f
    private lateinit var shakeAnim: Animation
    private val bottomSheet = MusicBottomSheet { music ->
        finishChooseMusic(music)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // khởi tạo musicPlayer mặc định để tránh crash
        musicPlayer = ExoPlayer.Builder(this).build()
        shakeAnim = AnimationUtils.loadAnimation(this, R.anim.shake)
        setupUI()

        with(binding) {
            etEditableText.post {
                originalX = etEditableText.x
                originalY = etEditableText.y
            }
        }

        with(binding) {
            blockMusic.post {
                originalMusicX = blockMusic.x
                originalMusicY = blockMusic.y
            }
        }

        // ----- Click listener ngắn gọn (gọi từ UI) -----
        binding.btnSharedStory.setOnClickListener {

            // nếu mà là ảnh
            if (isSelectedImage) {
                if (::musicPlayer.isInitialized) {
                    musicPlayer.stop()
                    musicPlayer.release()
                }
                binding.btnSharedStory.visibility = View.GONE
                // Tạo đối tượng Story
                val userId = SharedPrefer.getId()
                val story = Story(
                    userId = userId, music = musicSelected, durationMs = parseTimeToMillis(
                        musicSelected?.duration ?: "0:07"
                    )
                )
                Toast.makeText(this, "Tin của bạn đang được đăng", Toast.LENGTH_SHORT)
                    .show()
                binding.blockImage.post {
                    imgService.captureAndUpload(
                        binding.blockImage,
                        story
                    ) {
                        Toast.makeText(this, "Đăng tin thất bại!", Toast.LENGTH_SHORT).show()
                    }
                }
                finish()
            } else {
                // nếu mà là video
                Toast.makeText(this@StoryActivity, "Đang xử lí video", Toast.LENGTH_LONG).show()
                binding.btnSharedStory.visibility = View.GONE
                if (::musicPlayer.isInitialized) {
                    musicPlayer.stop()
                    musicPlayer.release()
                }

                val userId = SharedPrefer.getId()
                val story = Story(userId = userId, music = musicSelected)

                // đảm bảo view đã layout
                binding.blockImage.post {
                    try {
                        // 1) tạo overlay bitmap (sử dụng hàm bạn có)
                        val overlayBitmap = createOverlayWithHole(
                            binding.blockImage,
                            binding.videoTexture,
                            binding.etEditableText
                        )
                        val overlayFile = saveOverlayBitmapToFile(
                            this,
                            overlayBitmap,
                            "overlay_tmp_${System.currentTimeMillis()}.png"
                        )

                        // 2) tính các kích thước & vị trí
                        val blockW = binding.blockImage.width
                        val blockH = binding.blockImage.height

                        // IMPORTANT: lấy vị trí và kích thước thực tế của videoTexture *trong blockImage*
                        // nếu texture nằm trực tiếp bên trong blockImage và không có translation parent, thì:
                        val tx = binding.videoTexture.left
                        val ty = binding.videoTexture.top
                        val tw = binding.videoTexture.width
                        val th = binding.videoTexture.height

                        // 3) duration video
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(this, selectedUri)
                        val durStr =
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        val durationMs = durStr?.toLongOrNull() ?: 0L
                        retriever.release()

                        // 4) start service
                        val intent = Intent(this, VideoRenderService::class.java).apply {
                            putExtra(IntentExtras.EXTRA_VIDEO_URI, selectedUri)
                            putExtra(
                                IntentExtras.EXTRA_OVERLAY_PATH,
                                overlayFile.absolutePath
                            )
                            putExtra(IntentExtras.EXTRA_BLOCK_W, blockW)
                            putExtra(IntentExtras.EXTRA_BLOCK_H, blockH)
                            putExtra(IntentExtras.EXTRA_TX, tx)
                            putExtra(IntentExtras.EXTRA_TY, ty)
                            putExtra(IntentExtras.EXTRA_TW, tw)
                            putExtra(IntentExtras.EXTRA_TH, th)
                            putExtra(IntentExtras.EXTRA_DURATION_MS, durationMs)
                            putExtra(IntentExtras.EXTRA_STORY, story)
                        }

                        // 🔹 1. Kiểm tra và xin quyền thông báo (Android 13+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                                != PackageManager.PERMISSION_GRANTED
                            ) {
                                requestPermissions(
                                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                    1001
                                )
                                return@post
                            }
                        }
                        startForegroundService(intent)

                        // kết thúc luôn Activity
                        finish()

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

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

        binding.etEditableText.setOnTouchListener(object : View.OnTouchListener {

            private var dX = 0f
            private var dY = 0f

            @SuppressLint("SetTextI18n")
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.action) {

                    MotionEvent.ACTION_DOWN -> {
                        dX = view.x - event.rawX
                        dY = view.y - event.rawY

                        // hiển thị thùng rác
                        binding.btnTrash.visibility = View.VISIBLE
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

                        // kiểm tra xem là có chạm thùng rác hay không
                        val trashRect = Rect()
                        binding.btnTrash.getHitRect(trashRect)
                        if (trashRect.contains(
                                (view.x + view.width / 2).toInt(),
                                (view.y + view.height / 2).toInt()
                            )
                        ) {
                            // chạy hiệu ứng rung thùng rác
                            binding.btnTrash.startAnimation(shakeAnim)
                            // reset bằng animation để không bị nhảy đột ngột
                            binding.etEditableText.animate()
                                .alpha(0f)
                                .setDuration(120)
                                .withEndAction {
                                    // sau khi đã ẩn về mặt hiển thị
                                    binding.etEditableText.alpha =
                                        1f   // reset alpha để lần sau hiện lại bình thường
                                    binding.etEditableText.translationX = 0f
                                    binding.etEditableText.translationY = 0f
                                    binding.etEditableText.x = originalX
                                    binding.etEditableText.y = originalY
                                    binding.etEditableText.setText("Hãy viết gì đó...")
                                    binding.etEditableText.visibility = View.INVISIBLE
                                }.start()

                        }
                        // Ẩn thùng rác khi kết thúc kéo
                        binding.btnTrash.visibility = View.GONE
                    }
                }
                return true
            }
        })

        binding.blockMusic.setOnTouchListener(object : View.OnTouchListener {
            private var dX = 0f
            private var dY = 0f

            @SuppressLint("SetTextI18n")
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dX = view.x - event.rawX
                        dY = view.y - event.rawY

                        // hiển thị thùng rác
                        binding.btnTrash.visibility = View.VISIBLE
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
                        // chú ý đoạn này
                        overlayInfoMusic.posX = view.x
                        overlayInfoMusic.posY = view.y

                        // kiểm tra xem là có chạm thùng rác hay không
                        val trashRect = Rect()
                        binding.btnTrash.getHitRect(trashRect)
                        if (trashRect.contains(
                                (view.x + view.width / 2).toInt(),
                                (view.y + view.height / 2).toInt()
                            )
                        ) {
                            // chạy hiệu ứng rung thùng rác
                            binding.btnTrash.startAnimation(shakeAnim)

                            // tắt nhạc và xóa nhạc mà được chọn
                            musicPlayer.stop()
                            musicPlayer.release()
                            musicSelected = null

                            // reset bằng animation để không bị nhảy đột ngột
                            binding.blockMusic.animate()
                                .alpha(0f)
                                .setDuration(120)
                                .withEndAction {
                                    // sau khi đã ẩn về mặt hiển thị
                                    binding.blockMusic.alpha =
                                        1f   // reset alpha để lần sau hiện lại bình thường
                                    binding.blockMusic.translationX = 0f
                                    binding.blockMusic.translationY = 0f
                                    binding.blockMusic.x = originalMusicX
                                    binding.blockMusic.y = originalMusicY
                                    binding.blockMusic.visibility = View.INVISIBLE
                                }.start()
                        }
                        // Ẩn thùng rác khi kết thúc kéo
                        binding.btnTrash.visibility = View.GONE
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
        binding.btnTextJosefinSans.setOnClickListener {
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
            isSelectedImage = true
            showImagePreview(uri)
        } else if (contentType?.startsWith("video/") == true) {
            isSelectedImage = false
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
                binding.btnTextJosefinSans.setBackgroundResource(R.drawable.bg_text_style_selection) // bg_new.xml
                binding.btnTextJosefinSans.setTextColor(Color.BLACK)
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
                binding.btnTextJosefinSans.setBackgroundResource(R.drawable.bg_text_style) // bg_new.xml
                binding.btnTextJosefinSans.setTextColor(Color.WHITE)
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

    private fun createOverlayWithHole(
        blockView: View,
        textureView: TextureView,
        textView: View
    ): Bitmap {
        // ensure laid out
        if (blockView.width == 0 || blockView.height == 0) {
            throw IllegalStateException("blockView not laid out yet")
        }

        val blockW = blockView.width
        val blockH = blockView.height

        // Tọa độ textureView nằm trong blockView (blockView là parent trong layout của bạn)
        val videoLeft = textureView.left
        val videoTop = textureView.top
        val videoW = textureView.width
        val videoH = textureView.height

        // Tạo bitmap result (ARGB_8888 để có alpha)
        val result = createBitmap(blockW, blockH)
        val canvas = Canvas(result)

        // 1) Vẽ background của blockView (nếu có) lên canvas
        val bg = blockView.background
        if (bg != null) {
            bg.setBounds(0, 0, blockW, blockH)
            bg.draw(canvas)
        } else {
            // fallback: fill black if no background
            canvas.drawColor(Color.BLACK)
        }

        // Đoạn này liên quan đến việc mà vẽ những thành phần View còn bên trong ViewGroup

//        if (blockView is ViewGroup) {
//            for (i in 0 until blockView.childCount) {
//                val child = blockView.getChildAt(i)
//
//                // bỏ textureView (video) + bỏ cả textView
//                if (child === textureView || child === textView) continue
//
//                canvas.save()
//                canvas.translate(child.left.toFloat(), child.top.toFloat())
//                child.draw(canvas)
//                canvas.restore()
//            }
//        }

        // 3) MAKE HOLE: xóa vùng video để làm trong suốt (để video bên dưới hiện ra)
        val clearPaint = Paint()
        clearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        canvas.drawRect(
            videoLeft.toFloat(),
            videoTop.toFloat(),
            (videoLeft + videoW).toFloat(),
            (videoTop + videoH).toFloat(),
            clearPaint
        )
        // important: reset xfermode (not strictly necessary here)
        clearPaint.xfermode = null

        // 4) VẼ textView lên canvas (nếu nằm trên vùng video, nó vẽ lên trên)
        // (Một số layouts có thể đã vẽ text khi loop child; nếu chưa, vẽ lại để chắc chắn vị trí layer đúng)
        canvas.save()
        canvas.translate(overlayInfo.posX, overlayInfo.posY)
        textView.draw(canvas)
        canvas.restore()

        return result
    }

    private fun saveOverlayBitmapToFile(
        context: Context,
        bmp: Bitmap,
        filename: String = "overlay.png"
    ): File {
        val file = File(context.filesDir, filename)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Quyền được cấp, chạy service
            val intent = Intent(this, VideoRenderService::class.java)
            startForegroundService(intent)
        }
    }

    fun parseTimeToMillis(timeString: String): Long {
        try {
            val parts = timeString.split(":")
            if (parts.size != 2) return 0L

            val minutes = parts[0].toLongOrNull() ?: 0L
            val seconds = parts[1].toLongOrNull() ?: 0L

            return (minutes * 60 + seconds) * 1000
        } catch (e: Exception) {
            return 0L
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
