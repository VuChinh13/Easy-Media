package com.example.easymedia.ui.component.viewstory

import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.easymedia.R
import com.example.easymedia.data.model.Music
import com.example.easymedia.data.model.Story
import com.example.easymedia.databinding.ActivityViewStoryBinding
import com.example.easymedia.ui.component.utils.IntentExtras
import jp.shts.android.storiesprogressview.StoriesProgressView

class ViewStoryActivity : AppCompatActivity(), StoriesProgressView.StoriesListener {
    private lateinit var binding: ActivityViewStoryBinding
    private lateinit var listStory: List<Story>
    private var mediaPlayer: MediaPlayer? = null
    private val stories = listOf(
        "https://placekitten.com/1080/1920",
        "https://placebear.com/1080/1920",
        "https://picsum.photos/1080/1920"
    )

    private var counter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listStory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(IntentExtras.EXTRA_DATA_STORY, Story::class.java)
                ?: emptyList()
        } else {
            intent.getParcelableArrayListExtra<Story>(IntentExtras.EXTRA_DATA_STORY) ?: emptyList()
        }

        Glide.with(this).load(listStory[0].imageUrl)
            .error(R.drawable.ic_avatar)
            .into(binding.imageStory)

        playMusicOnce(listStory[0].music ?: Music())

        binding.storiesProgressView.setStoriesCount(stories.size)
        binding.storiesProgressView.setStoryDuration(5000L)
        binding.storiesProgressView.setStoriesListener(this)
        binding.storiesProgressView.startStories()


        loadImage(stories[counter])

        // Nhấn giữ để tạm dừng
//        binding.imageStory.setOnTouchListener { _, event ->
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> binding.storiesProgressView.pause()
//                MotionEvent.ACTION_UP -> binding.storiesProgressView.resume()
//            }
//            true
//        }
    }


    fun playMusicOnce(music: Music) {
        if (music.url.isEmpty()) return

        try {
            mediaPlayer?.release() // giải phóng MediaPlayer cũ nếu có
            mediaPlayer = MediaPlayer().apply {
                setDataSource(music.url)
                setOnPreparedListener {
                    it.start()  // bắt đầu phát
                }
                setOnCompletionListener {
                    it.release()
                    Log.d("MusicPlayer", "Finished playing: ${music.title}")
                    mediaPlayer = null
                }
                prepareAsync() // chuẩn bị không chặn UI thread
            }
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Error playing music: ${e.message}", e)
        }
    }

    private fun loadImage(url: String) {
        // nếu bạn có Glide:
        // Glide.with(this).load(url).into(binding.imageStory)
    }

    override fun onNext() {
        counter++
        if (counter < stories.size) loadImage(stories[counter])
    }

    override fun onPrev() {
        counter--
        if (counter >= 0) loadImage(stories[counter])
    }

    override fun onComplete() {
        finish()
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        binding.storiesProgressView.destroy()
        super.onDestroy()
    }
}