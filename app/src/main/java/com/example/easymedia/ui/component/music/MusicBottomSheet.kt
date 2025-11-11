package com.example.easymedia.ui.component.music

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.media.MediaPlayer
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.easymedia.R
import com.example.easymedia.data.model.Music
import com.example.easymedia.databinding.MusicBottomsheetBinding
import com.example.easymedia.ui.component.music.adapter.MusicAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MusicBottomSheet : BottomSheetDialogFragment() {
    private var listMusic = mutableListOf<Music>()
    private lateinit var binding: MusicBottomsheetBinding
    private lateinit var adapter: MusicAdapter
    private var mediaPlayer: MediaPlayer? = null
    private var currentMusic: Music? = null
    private var isPlaying = true
    private var pausePosition: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = MusicBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog as? BottomSheetDialog
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                val displayMetrics = resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels
                val desiredHeight = (screenHeight * 0.8).toInt()

                val layoutParams = sheet.layoutParams
                layoutParams.height = desiredHeight
                sheet.layoutParams = layoutParams

                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.maxHeight = desiredHeight // chỉ API 34+, nếu thấp hơn thì bỏ
            }

        val searchView = binding.btnSearchMusic
        val searchEditText = searchView.findViewById<AutoCompleteTextView>(
            androidx.appcompat.R.id.search_src_text
        )
        searchEditText.setTextColor(Color.WHITE)
        searchEditText.textSize = 17f
        val searchIcon = searchView.findViewById<ImageView>(
            androidx.appcompat.R.id.search_mag_icon
        )
        searchIcon.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)  // đổi màu icon
        val closeButton = searchView.findViewById<ImageView>(
            androidx.appcompat.R.id.search_close_btn
        )
        closeButton.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)  // đổi màu
        searchEditText.setHintTextColor(Color.LTGRAY)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MusicAdapter(listMusic) { selectedMusic ->
            playMusic(selectedMusic)
        }

        binding.rcvMusic.layoutManager = LinearLayoutManager(context)
        binding.rcvMusic.adapter = adapter

        setupSearchView()

        binding.btnPause.setOnClickListener {
            if (isPlaying) {
                // Tạm dừng nhạc
                isPlaying = false
                binding.btnPause.setImageResource(R.drawable.ic_play)
                mediaPlayer?.pause()
                pausePosition = mediaPlayer?.currentPosition ?: 0
            } else {
                // Tiếp tục phát nhạc từ chỗ dừng
                isPlaying = true
                binding.btnPause.setImageResource(R.drawable.ic_pause_media)
                mediaPlayer?.seekTo(pausePosition)
                mediaPlayer?.start()
            }
        }
    }

    private fun setupSearchView() {
        val searchView = binding.btnSearchMusic

        // Lắng nghe sự kiện gõ chữ trong SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                adapter.filter(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText.orEmpty())
                return true
            }
        })
    }

    private fun playMusic(music: Music) {
        isPlaying = true
        // Nếu đang phát bài khác → dừng lại
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        currentMusic = music
        mediaPlayer = MediaPlayer().apply {
            setDataSource(music.url)
            setOnPreparedListener { start() }
            setOnCompletionListener {
                // Có thể thêm auto-next ở đây
            }
            setOnErrorListener { _, what, extra ->
                false
            }
            prepareAsync() // load nhạc online bất đồng bộ
        }

        binding.blockPlaying.visibility = View.VISIBLE
        binding.tvTitle.text = music.title
        binding.tvArtist.text = music.artist
    }

    fun updateListMusic(newListMusic: MutableList<Music>) {
        listMusic.clear()
        listMusic = newListMusic
        adapter.updateData(this.listMusic)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        // 👉 Viết logic ở đây khi BottomSheet đóng
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

}
