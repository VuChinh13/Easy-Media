package com.example.easymedia.ui.component.story.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.antonkarpenko.ffmpegkit.FFmpegKit
import com.antonkarpenko.ffmpegkit.FFmpegKitConfig
import com.antonkarpenko.ffmpegkit.LogCallback
import com.antonkarpenko.ffmpegkit.ReturnCode
import com.antonkarpenko.ffmpegkit.StatisticsCallback
import com.example.easymedia.data.data_source.cloudinary.CloudinaryServiceImpl
import com.example.easymedia.data.data_source.firebase.FirebaseStoryService
import com.example.easymedia.data.model.Story
import com.example.easymedia.data.repository.StoryRepositoryImpl
import com.example.easymedia.ui.component.utils.IntentExtras
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL

class VideoRenderService : Service() {

    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "video_render_channel"
    private val TAG = "VideoRenderService"
    private lateinit var notificationManager: NotificationManager
    private val storyRepository =
        StoryRepositoryImpl(FirebaseStoryService(cloudinary = CloudinaryServiceImpl()))

    private var story: Story? = Story()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val videoUri = intent?.getParcelableExtra<Uri>(IntentExtras.EXTRA_VIDEO_URI)
        val overlayPath = intent?.getStringExtra(IntentExtras.EXTRA_OVERLAY_PATH)
        val blockW = intent?.getIntExtra(IntentExtras.EXTRA_BLOCK_W, 0) ?: 0
        val blockH = intent?.getIntExtra(IntentExtras.EXTRA_BLOCK_H, 0) ?: 0
        val tx = intent?.getIntExtra(IntentExtras.EXTRA_TX, 0) ?: 0
        val ty = intent?.getIntExtra(IntentExtras.EXTRA_TY, 0) ?: 0
        val isDownload = intent?.getBooleanExtra(IntentExtras.EXTRA_DOWNLOAD, false) ?: false
        val tw = intent?.getIntExtra(IntentExtras.EXTRA_TW, 0) ?: 0
        val th = intent?.getIntExtra(IntentExtras.EXTRA_TH, 0) ?: 0
        story = intent?.getParcelableExtra<Story>(IntentExtras.EXTRA_STORY)
        val durationMs = intent?.getLongExtra(IntentExtras.EXTRA_DURATION_MS, 0L) ?: 0L


        val isMuted = intent?.getBooleanExtra(IntentExtras.EXTRA_MUTED, false) ?: false
        val musicSelected = intent?.getStringExtra(IntentExtras.EXTRA_MUSIC) ?: ""
        val musicActualDurationMs =
            intent?.getLongExtra(IntentExtras.EXTRA_DURATION_MUSIC, 0L) ?: 0L
        val isMusicClipped =
            intent?.getBooleanExtra(IntentExtras.EXTRA_MUSIC_CLIPPED, false) ?: false

        if (videoUri == null || overlayPath.isNullOrEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        // start foreground with 0% progress
        startForeground(NOTIFICATION_ID, createNotification(0))

        Thread {
            try {
                renderVideo(
                    videoUri,
                    File(overlayPath),
                    blockW,
                    blockH,
                    tx,
                    ty,
                    tw,
                    th,
                    durationMs,
                    isMuted,
                    musicSelected,
                    musicActualDurationMs,
                    isMusicClipped,
                    isDownload
                )
            } catch (e: Exception) {
                Log.e(TAG, "Render failed", e)
            } finally {
                stopForeground(true)
                stopSelf()
            }
        }.start()

        return START_NOT_STICKY
    }

    private fun createNotification(progress: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Đang xử lý video...")
            .setContentText(if (progress == 0) "Chuẩn bị..." else "Progress: $progress%")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setProgress(100, progress, false)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Render",
                NotificationManager.IMPORTANCE_HIGH   // CHỈNH Ở ĐÂY
            ).apply {
                description = "Notification for video rendering"
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun renderVideo(
        videoUri: Uri,
        overlayFile: File,
        blockW: Int,
        blockH: Int,
        tx: Int,
        ty: Int,
        tw: Int,
        th: Int,
        durationMs: Long,
        isMuted: Boolean,
        musicSelected: String,
        musicActualDurationMs: Long,
        isMusicClipped: Boolean,
        isDownload: Boolean
    ) {
        try {

            // -------------------------
            // 1) COPY VIDEO INPUT
            // -------------------------
            val inputVideo = File(cacheDir, "input_video_${System.currentTimeMillis()}.mp4")
            contentResolver.openInputStream(videoUri)?.use { input ->
                inputVideo.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // -------------------------
            // 2) TẢI MUSIC TỪ URL (NẾU CÓ)
            // -------------------------
            var musicFile: File? = null
            val hasMusic = musicSelected.isNotEmpty()

            if (hasMusic) {
                try {
                    val url = URL(musicSelected)
                    musicFile = File(cacheDir, "music_${System.currentTimeMillis()}.mp3")
                    url.openStream().use { input ->
                        FileOutputStream(musicFile!!).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "Downloaded music: ${musicFile!!.absolutePath}")
                } catch (e: Exception) {
                    Log.e(TAG, "Music download failed: $e")
                    musicFile = null
                }
            }

            val outputVideo = File(cacheDir, "output_render_${System.currentTimeMillis()}.mp4")

            // -------------------------
            // 3) VIDEO FILTER (scale + pad + overlay)
            // -------------------------
            val videoFilter =
                "[0:v]scale=${tw}:${th}[sv];" +
                        "[sv]pad=${blockW}:${blockH}:${tx}:${ty}:color=0x00000000[bg];" +
                        "[bg][1:v]overlay=0:0[ov];" +
                        "[ov]scale=720:-2[outv]"

            // -------------------------
            // 4) XỬ LÍ AUDIO LOGIC
            // -------------------------
            val cmd = mutableListOf<String>()

            cmd += listOf("-y")
            cmd += listOf("-i", inputVideo.absolutePath)   // 0:v + 0:a
            cmd += listOf("-i", overlayFile.absolutePath)  // 1:v

            var audioCodec = ""
            var audioMapping = ""
            var musicInputIndex = -1

            // ----------- CASE 1: mặc định (giữ tiếng video)
            if (!isMuted && !hasMusic) {
                audioCodec = "-c:a aac"
                audioMapping = "-map 0:a"
            }

            // ----------- CASE 2: tắt tiếng video, không có nhạc
            if (isMuted && !hasMusic) {
                audioCodec = "-an"
            }

            // ----------- CASE 3 + 4: có nhạc
            if (isMuted && hasMusic && musicFile != null) {
                cmd += listOf("-i", musicFile.absolutePath) // input index 2
                musicInputIndex = 2

                audioCodec = "-c:a aac"

                if (isMusicClipped) {
                    audioMapping = "-map $musicInputIndex:a -t ${musicActualDurationMs / 1000.0}"
                } else {
                    audioMapping = "-map $musicInputIndex:a"
                }
            }

            // -------------------------
            // 5) GHÉP VIDEO FILTER
            // -------------------------
            cmd += listOf("-filter_complex", videoFilter)
            cmd += listOf("-map", "[outv]")

            cmd += listOf("-c:v", "libx264")
            cmd += listOf("-preset", "ultrafast")
            cmd += listOf("-crf", "20")
            cmd += listOf("-b:v", "8000k")
            cmd += listOf("-maxrate", "8000k")
            cmd += listOf("-bufsize", "12000k")

            // audio mapping
            if (audioMapping.isNotEmpty()) {
                cmd += audioMapping.split(" ")
            }
            // audio codec
            if (audioCodec.isNotEmpty()) {
                cmd += audioCodec.split(" ")
            }

            // -------------------------
            // 6) GIỚI HẠN VIDEO 60 GIÂY
            // -------------------------
            val maxDurationSec = 60

            if (durationMs > maxDurationSec * 1000L) {
                cmd += listOf("-t", maxDurationSec.toString())
            }

            // movflags
            cmd += listOf("-movflags", "+faststart")
            cmd += listOf(outputVideo.absolutePath)


            // -------------------------
            // 6) LOGGING
            // -------------------------
            Log.d(TAG, "FFmpeg CMD:\n${cmd.joinToString(" ")}")

            FFmpegKitConfig.enableLogCallback(LogCallback { log ->
                Log.d(TAG, "FFmpegLog: ${log.message}")
            })

            FFmpegKitConfig.enableStatisticsCallback(StatisticsCallback { stats ->
                val timeMs = stats.time
                if (durationMs > 0) {
                    val percent =
                        ((timeMs.toDouble() / durationMs.toDouble()) * 100).toInt().coerceIn(0, 100)
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        createNotification(percent)
                    )
                }
            })

            // -------------------------
            // 7) RUN FFmpeg
            // -------------------------
            FFmpegKit.executeAsync(cmd.joinToString(" ")) { session ->
                val returnCode = session.returnCode

                if (ReturnCode.isSuccess(returnCode)) {
                    Log.d(TAG, "FFmpeg success: ${outputVideo.absolutePath}")

                    // nếu mà là true thì lưu video
                    if (isDownload) {
                        val savedUri = saveVideoToPublic(outputVideo)
                        Log.d(TAG, "Saved to gallery: $savedUri")
                    }

                    // upload story
                    story?.let { st ->
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                st.durationMs = getVideoDuration(outputVideo)
                                val result = storyRepository.uploadStory(st, outputVideo, true)

                                if (result) {
                                    val notif = NotificationCompat.Builder(
                                        this@VideoRenderService,
                                        CHANNEL_ID
                                    )
                                        .setContentTitle("Tin đang được đăng")
                                        .setContentText("Xử lý xong.")
                                        .setSmallIcon(android.R.drawable.ic_media_play)
                                        .build()
                                    notificationManager.notify(NOTIFICATION_ID, notif)

                                    val intent = Intent("com.example.easymedia.UPLOAD_DONE")
                                    intent.putExtra(IntentExtras.RESULT_DATA_STR, true)
                                    sendBroadcast(intent)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Upload failed", e)
                            }
                        }
                    }

                } else {
                    Log.e(TAG, "FFmpeg failed: $returnCode")
                    Log.e(TAG, session.allLogsAsString)

                    val err = NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Lỗi xử lý video")
                        .setContentText("Vui lòng thử lại.")
                        .setSmallIcon(android.R.drawable.stat_notify_error)
                        .build()

                    notificationManager.notify(NOTIFICATION_ID, err)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception renderVideo", e)
        }
    }

    private fun saveVideoToPublic(source: File): Uri? {
        return try {
            val resolver = contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "story_${System.currentTimeMillis()}.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/EasyMedia")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val uri = resolver.insert(collection, values)!!
            resolver.openOutputStream(uri).use { out ->
                FileInputStream(source).use { input ->
                    input.copyTo(out!!)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } catch (e: Exception) {
            Log.e(TAG, "saveVideoToPublic failed", e)
            null
        }
    }

    private fun getVideoDuration(videoFile: File): Long {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoFile.absolutePath)
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        return time?.toLong() ?: 0L
    }

    override fun onDestroy() {
        super.onDestroy()

        // FFmpegKit Anton bản 2.1.0 KHÔNG có hàm disable → chỉ có cách set null
        FFmpegKitConfig.enableStatisticsCallback(null)
        FFmpegKitConfig.enableLogCallback(null)
    }

}
