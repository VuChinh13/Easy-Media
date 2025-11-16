package com.example.easymedia.ui.component.viewstory

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.example.easymedia.R
import com.example.easymedia.data.model.Music
import com.example.easymedia.data.model.Story
import com.example.easymedia.databinding.ActivityViewStoryBinding
import com.example.easymedia.ui.component.utils.IntentExtras
import com.example.easymedia.ui.component.utils.TimeFormatter
import jp.shts.android.storiesprogressview.StoriesProgressView

class ViewStoryActivity : AppCompatActivity(), StoriesProgressView.StoriesListener {
    private val viewStoryViewModel: ViewStoryViewModel by viewModels()
    private lateinit var binding: ActivityViewStoryBinding
    private var exoPlayer: ExoPlayer? = null
    private var listStory: List<Story> = emptyList()
    private val durations = mutableListOf<Long>()

    // touch press handling
    private var pressTime = 0L
    private val limit = 500L // giữ 0.5s mới tạm dừng

    // media
    private var mediaPlayer: MediaPlayer? = null

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

        // load thông tin user (ví dụ hiển thị avatar/username)
        viewStoryViewModel.getInforUser(listStory[0].userId)
        viewStoryViewModel.getInforUserResult.observe(this) { user ->
            Glide.with(this)
                .load(user?.profilePicture)
                .error(R.drawable.ic_avatar)
                .into(binding.imageProfile)
            binding.tvUsername.text = user?.username ?: ""
        }

        // show story đầu tiên
        counter = 0
        loadStoryAt(counter)

        // chuẩn bị durations (mm:ss -> millis)
        prepareDurations()
        // set số thanh (nếu cần) và durations
        binding.storiesProgressView.setStoriesCount(listStory.size)
        binding.storiesProgressView.setStoriesCountWithDurations(durations.toLongArray())

        // listener và start
        binding.storiesProgressView.setStoriesListener(this)
        binding.storiesProgressView.startStories()

        // touch: nhấn giữ để tạm dừng, chạm nhẹ để skip
        binding.imageStory.setOnTouchListener { view, event ->
            val width = view.width
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    pressTime = System.currentTimeMillis()
                    binding.storiesProgressView.pause()
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
                            binding.storiesProgressView.skip()
                        }
                    } else {
                        binding.storiesProgressView.resume()
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
                    binding.storiesProgressView.pause()
                    exoPlayer?.pause()
                }

                MotionEvent.ACTION_UP -> {
                    val duration = System.currentTimeMillis() - pressTime
                    if (duration < limit) {
                        if (event.x < width / 2f) {
                            // tap bên trái → quay lại story trước
                            reverseStoryProperly()
                        } else {
                            // tap bên phải → tới story sau
                            binding.storiesProgressView.skip()
                        }
                    } else {
                        binding.storiesProgressView.resume()
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

        // Khi playback kết thúc → skip story tiếp theo
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    binding.storiesProgressView.skip()
                }
            }
        })
    }

    private fun loadStoryAt(index: Int) {
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
    }

    private fun prepareDurations() {
        durations.clear()
        listStory.forEach { story ->
            durations.add(story.durationMs)
        }
        // đảm bảo kích thước khớp (thư viện yêu cầu)
        if (durations.size != listStory.size) {
            // fallback: nếu có bất đồng, gán mặc định 5s cho tất cả
            durations.clear()
            repeat(listStory.size) { durations.add(5000L) }
        }
    }

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
                    // chỉ giải phóng; không tự move story (StoriesProgressView điều khiển chuyển story theo duration)
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
            // resume nếu đã có instance và chưa hoàn tất
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

    // ----- StoriesProgressView callbacks -----

    override fun onNext() {
        // storiesProgressView đã chuyển sang story tiếp theo
        counter++
        if (counter < listStory.size) {
            loadStoryAt(counter)
        } else {
            // index vượt quá, bảo đảm kết thúc
            onComplete()
        }
    }

    override fun onPrev() {
        // người dùng chuyển về story trước
        counter--
        if (counter >= 0) {
            loadStoryAt(counter)
        } else {
            // nếu âm, set lại 0
            counter = 0
            loadStoryAt(counter)
        }
    }

    override fun onComplete() {
        // khi chạy hết stories
        finish()
    }

    override fun onDestroy() {
        releaseMediaPlayer()
        binding.storiesProgressView.destroy()
        exoPlayer?.release()
        exoPlayer = null
        binding.storiesProgressView.destroy()
        super.onDestroy()
    }

    // ----- utility -----

    private fun parseDurationToMillis(durationStr: String): Long {
        if (durationStr.isBlank()) return 5000L // fallback mặc định 5s

        return try {
            val parts = durationStr.split(":")
            when (parts.size) {
                2 -> {
                    val minutes = parts[0].toLongOrNull() ?: 0L
                    val seconds = parts[1].toLongOrNull() ?: 0L
                    (minutes * 60 + seconds) * 1000
                }

                1 -> {
                    val seconds = parts[0].toLongOrNull() ?: 0L
                    seconds * 1000
                }

                else -> 5000L
            }
        } catch (e: Exception) {
            5000L
        }
    }

    private fun reverseStoryProperly() {
        if (counter > 0) {
            counter--

            // Reset lại storiesProgressView từ đầu
            binding.storiesProgressView.destroy()

            binding.storiesProgressView.setStoriesCount(listStory.size)
            binding.storiesProgressView.setStoriesCountWithDurations(durations.toLongArray())
            binding.storiesProgressView.setStoriesListener(this)

            // Start từ story 0
            binding.storiesProgressView.startStories()

            // Skip đến story cần hiển thị
            for (i in 0 until counter) {
                binding.storiesProgressView.skip()
            }

            loadStoryAt(counter)
        }
    }

}
