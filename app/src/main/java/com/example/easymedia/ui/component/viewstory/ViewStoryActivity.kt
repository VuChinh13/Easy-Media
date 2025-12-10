package com.example.easymedia.ui.component.viewstory

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.example.easymedia.R
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.data.data_source.firebase.FirebaseStoryService
import com.example.easymedia.data.model.Music
import com.example.easymedia.data.model.Story
import com.example.easymedia.data.repository.StoryRepositoryImpl
import com.example.easymedia.databinding.ActivityViewStoryBinding
import com.example.easymedia.ui.component.utils.IntentExtras
import com.example.easymedia.ui.component.utils.SharedPrefer
import com.example.easymedia.ui.component.utils.TimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewStoryActivity : AppCompatActivity() {
    private val viewStoryViewModel: ViewStoryViewModel by viewModels()
    private lateinit var binding: ActivityViewStoryBinding
    private var exoPlayer: ExoPlayer? = null
    private var listStory: List<Story> = emptyList()
    private val durations = mutableListOf<Long>()
    private val repository =
        StoryRepositoryImpl(FirebaseStoryService(cloudinary = CloudinaryServiceImpl()))

    // progress views
    private val progressBgs = mutableListOf<View>()
    private val progressFills = mutableListOf<View>()

    // animator
    private var animator: ValueAnimator? = null

    // touch press handling
    private var pressTime = 0L
    private val limit = 500L // giữ 0.5s mới tạm dừng

    // media
    private var mediaPlayer: MediaPlayer? = null
    private val userId = SharedPrefer.getId()

    // index hiện tại (story đang hiển thị)
    private var counter = 0

    private fun initPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        binding.videoStory.player = exoPlayer
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lấy dữ liệu từ intent (an toàn cho cả API >= TIRAMISU)
        listStory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(IntentExtras.EXTRA_DATA_STORY, Story::class.java)
                ?: emptyList()
        } else {
            intent.getParcelableArrayListExtra<Story>(IntentExtras.EXTRA_DATA_STORY) ?: emptyList()
        }

        if (listStory.isEmpty()) {
            finish() // không có story thì đóng activity
            return
        }

        initPlayer()

        // Kiểm tra xem liệu là có phải là Story của mình không
        if (userId == listStory[0].userId) {
            binding.tbMenu.visibility = View.VISIBLE
            binding.tbMenu.inflateMenu(R.menu.menu_item_story)
            binding.tbMenu.overflowIcon?.setTint(Color.WHITE)
        }

        //Sự kiện xóa tin
        binding.tbMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_delete_story -> {
                    // Xử lý xóa story
                    releaseMediaPlayer()
                    clearAnimator()
                    exoPlayer?.release()
                    exoPlayer = null

                    binding.loadingOverlay.visibility = View.VISIBLE
                    lifecycleScope.launch(Dispatchers.IO) {
                        val result = repository.deleteStory(listStory[counter].id)
                        if (result) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@ViewStoryActivity,
                                    "Đã xóa tin của bạn",
                                    Toast.LENGTH_SHORT
                                ).show()

                                val resultIntent = Intent().apply {
                                    putExtra(IntentExtras.RESULT_DATA, true)
                                }
                                setResult(RESULT_OK, resultIntent)
                                finish()
                            }
                        }
                    }
                    true
                }

                else -> false
            }
        }


        // load thông tin user (ví dụ hiển thị avatar/username)
        viewStoryViewModel.getInforUser(listStory[0].userId)
        viewStoryViewModel.getInforUserResult.observe(this) { user ->
            Glide.with(this)
                .load(user?.profilePicture)
                .error(R.drawable.ic_avatar)
                .into(binding.imageProfile)
            binding.tvUsername.text = user?.username ?: ""

            // chuyển sang màn profile
            binding.tvUsername.setOnClickListener {
                val intent = Intent().apply {
                    putExtra(IntentExtras.EXTRA_USER, user)
                }
                setResult(RESULT_OK, intent)
                finish()
            }

            // chuyển sang màn profile
            binding.imageProfile.setOnClickListener {
                val intent = Intent().apply {
                    putExtra(IntentExtras.EXTRA_USER, user)
                }
                setResult(RESULT_OK, intent)
                finish()
            }

        }

        // chuẩn bị durations (mm:ss -> millis)
        prepareDurations()

        // tạo progress bar động
        setupProgressBars(listStory.size)

        // show story đầu tiên và start
        counter = 0
        loadStoryAt(counter)
        startProgressForCurrent()

        // touch: nhấn giữ để tạm dừng, chạm nhẹ để skip
        binding.imageStory.setOnTouchListener { view, event ->
            val width = view.width
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    pressTime = System.currentTimeMillis()
                    pauseStory()
                    pauseMedia()
                }

                MotionEvent.ACTION_UP -> {
                    val duration = System.currentTimeMillis() - pressTime
                    if (duration < limit) {
                        if (event.x < width / 2f) {
                            // tap bên trái → quay lại story trước
                            reverseStoryProperly()
                        } else {
                            // tap bên phải → tới story sau
                            skipStory()
                        }
                    } else {
                        resumeStory()
                        resumeMedia()
                    }
                }
            }
            true
        }

        binding.videoStory.setOnTouchListener { view, event ->
            val width = view.width
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    pressTime = System.currentTimeMillis()
                    pauseStory()
                    exoPlayer?.pause()
                }

                MotionEvent.ACTION_UP -> {
                    val duration = System.currentTimeMillis() - pressTime
                    if (duration < limit) {
                        if (event.x < width / 2f) {
                            reverseStoryProperly()
                        } else {
                            skipStory()
                        }
                    } else {
                        resumeStory()
                        exoPlayer?.play()
                    }
                }
            }
            true
        }
    }

    private fun isVideo(url: String): Boolean {
        return url.endsWith(".mp4")
    }

    private fun playVideo(url: String) {
        binding.videoStory.visibility = View.VISIBLE
        binding.imageStory.visibility = View.GONE

        val mediaItem = MediaItem.fromUri(url)

        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()      // chuẩn bị buffer ngay
            playWhenReady = true
        }

        // remove previous listener by resetting player listeners (safe approach)
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    skipStory()
                }
            }
        })
    }

    private fun loadStoryAt(index: Int) {
        // stop/clear exoplayer before set new
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()

        val story = listStory[index]
        binding.tvTime.text = TimeFormatter.formatTimeAgo(story.createdAt!!)

        if (isVideo(story.imageUrl)) {
            playVideo(story.imageUrl)
        } else {
            binding.videoStory.visibility = View.GONE
            binding.imageStory.visibility = View.VISIBLE
            playMusicForStory(story)
            Glide.with(this)
                .load(story.imageUrl)
                .error(R.drawable.ic_avatar)
                .into(binding.imageStory)
        }

        // cập nhật trạng thái progress visuals:
        // những fill trước index -> đầy, fill index -> giữ whatever, fill sau -> rỗng
        for (i in progressFills.indices) {
            val lp = progressFills[i].layoutParams
            if (i < index) {
                lp.width = (progressFills[i].parent as View).width
            } else if (i == index) {
                lp.width = 0
            } else {
                lp.width = 0
            }
            progressFills[i].layoutParams = lp
            progressFills[i].requestLayout()
        }
    }

    private fun prepareDurations() {
        durations.clear()
        listStory.forEach { story ->
            // nếu Story có field durationMs (bạn dùng), dùng nó
            durations.add(story.durationMs)
        }
        // đảm bảo kích thước khớp
        if (durations.size != listStory.size) {
            durations.clear()
            repeat(listStory.size) { durations.add(5000L) }
        }
    }

    // ----- Progress bar dynamic -----
    private fun setupProgressBars(count: Int) {
        progressBgs.clear()
        progressFills.clear()
        binding.progressContainer.removeAllViews()

        for (i in 0 until count) {
            val frame = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, 4.dp(), 1f).also {
                    val m = 2.dp()
                    (it as LinearLayout.LayoutParams).marginStart = m
                    it.marginEnd = m
                }
            }

            val bg = View(this).apply {
                setBackgroundColor("#898989".toColorInt())
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val fill = View(this).apply {
                setBackgroundColor(resources.getColor(R.color.white, nullOrGetTheme()))
                layoutParams = FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
            }

            frame.addView(bg)
            frame.addView(fill)

            binding.progressContainer.addView(frame)
            progressBgs.add(bg)
            progressFills.add(fill)
        }
    }

    // helper to get theme for color fetch across API
    private fun nullOrGetTheme() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) theme else null

    // Start animator for current index
    private fun startProgressForCurrent() {
        clearAnimator()

        // safety
        if (currentIndexOutOfRange()) return

        val fill = progressFills[counter]
        val parent = fill.parent as View
        val targetWidth = parent.width

        // if width not measured yet, post until measured
        if (targetWidth == 0) {
            parent.post { startProgressForCurrent() }
            return
        }

        // reset current fill to 0 before animating
        fill.layoutParams.width = 0
        fill.requestLayout()

        val duration = durations.getOrNull(counter) ?: 5000L

        animator = ValueAnimator.ofInt(0, targetWidth).apply {
            this.duration = duration
            addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                fill.layoutParams.width = value
                fill.requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // khi hoàn thành thanh hiện tại, đi next
                    // đảm bảo gọi next chỉ khi animator không bị cancel
                    if (!isCancelledAnimator(animation)) {
                        goToNextFromAnimator()
                    }
                }
            })
            start()
        }
    }

    private fun isCancelledAnimator(animation: Animator?) =
        // ValueAnimator không expose cancelled flag; check animator?.isRunning false and play state?
        animator == null || animator?.isRunning == false && animator?.animatedFraction != 1f

    private fun goToNextFromAnimator() {
        // tiến sang story tiếp theo
        if (counter < listStory.size - 1) {
            counter++
            loadStoryAt(counter)
            startProgressForCurrent()
        } else {
            onComplete()
        }
    }

    private fun skipStory() {
        // mark current fill full and go next
        if (currentIndexOutOfRange()) return
        val fill = progressFills[counter]
        val parent = fill.parent as View
        val fullWidth = parent.width
        // cancel animator first
        clearAnimator()
        fill.layoutParams.width = fullWidth
        fill.requestLayout()

        // next
        if (counter < listStory.size - 1) {
            counter++
            loadStoryAt(counter)
            startProgressForCurrent()
        } else {
            onComplete()
        }
    }

    private fun previousStory() {
        // cho tương tự Instagram: quay về story trước và bắt đầu từ 0
        if (counter > 0) {
            clearAnimator()
            // reset current fill to 0
            progressFills[counter].layoutParams.width = 0
            progressFills[counter].requestLayout()

            counter--
            // reset the target previous fill to 0 (we want to animate it)
            progressFills[counter].layoutParams.width = 0
            progressFills[counter].requestLayout()

            loadStoryAt(counter)
            startProgressForCurrent()
        } else {
            // nếu đang ở story đầu, restart nó
            clearAnimator()
            progressFills[0].layoutParams.width = 0
            progressFills[0].requestLayout()
            loadStoryAt(0)
            startProgressForCurrent()
        }
    }

    private fun reverseStoryProperly() {
        // lùi 1 story (same as previousStory but kept function name)
        previousStory()
    }

    private fun pauseStory() {
        try {
            animator?.pause()
        } catch (e: Exception) {
            // API fallback: cancel if pause not supported
            animator?.cancel()
        }
    }

    private fun resumeStory() {
        try {
            animator?.resume()
        } catch (e: Exception) {
            // nếu resume không có, restart từ vị trí hiện tại bằng cách lấy fraction -> estimate remaining time
            // fallback đơn giản: restart using remaining fraction
            animator?.let {
                if (!it.isRunning) {
                    startProgressForCurrent()
                }
            }
        }
    }

    private fun clearAnimator() {
        try {
            animator?.removeAllUpdateListeners()
            animator?.cancel()
        } catch (e: Exception) {
            // ignore
        } finally {
            animator = null
        }
    }

    private fun currentIndexOutOfRange() = counter < 0 || counter >= progressFills.size

    // ----- Media helpers -----
    private fun playMusicForStory(story: Story) {
        val music = story.music ?: Music()
        if (music.url.isNullOrBlank()) {
            // nếu không có nhạc thì giải phóng media player nếu đang có
            releaseMediaPlayer()
            return
        }

        try {
            // release old one
            releaseMediaPlayer()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(music.url)
                setOnPreparedListener {
                    it.start()
                    Log.d("MusicPlayer", "Started: ${music.title}")
                }
                setOnCompletionListener {
                    Log.d("MusicPlayer", "Completed: ${music.title}")
                    // chỉ giải phóng; không tự move story (chúng ta dùng duration của story)
                    it.release()
                    mediaPlayer = null
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Error playing music: ${e.message}", e)
            releaseMediaPlayer()
        }
    }

    private fun pauseMedia() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                Log.d("MusicPlayer", "Paused")
            }
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Error pausing media: ${e.message}", e)
        }
    }

    private fun resumeMedia() {
        try {
            mediaPlayer?.let {
                if (!it.isPlaying) {
                    it.start()
                    Log.d("MusicPlayer", "Resumed")
                }
            }
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Error resuming media: ${e.message}", e)
        }
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.let {
                try {
                    it.stop()
                } catch (_: Exception) { /* ignore stop errors */
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Error releasing media player: ${e.message}", e)
        } finally {
            mediaPlayer = null
        }
    }

    // ----- Completion / lifecycle -----
    private fun onComplete() {
        finish()
    }

    override fun onDestroy() {
        releaseMediaPlayer()
        clearAnimator()
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }

    // extension dp
    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}
