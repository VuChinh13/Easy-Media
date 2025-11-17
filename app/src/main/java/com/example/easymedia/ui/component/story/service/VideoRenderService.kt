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
import com.antonkarpenko.ffmpegkit.Statistics
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
        val tw = intent?.getIntExtra(IntentExtras.EXTRA_TW, 0) ?: 0
        val th = intent?.getIntExtra(IntentExtras.EXTRA_TH, 0) ?: 0
        story = intent?.getParcelableExtra<Story>(IntentExtras.EXTRA_STORY)
        val durationMs = intent?.getLongExtra(IntentExtras.EXTRA_DURATION_MS, 0L) ?: 0L

        if (videoUri == null || overlayPath.isNullOrEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        // start foreground with 0% progress
        startForeground(NOTIFICATION_ID, createNotification(0))

        Thread {
            try {
                renderVideo(videoUri, File(overlayPath), blockW, blockH, tx, ty, tw, th, durationMs)
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
        durationMs: Long
    ) {
        try {
            // Copy input to cache
            val inputVideo = File(cacheDir, "input_video_${System.currentTimeMillis()}.mp4")
            contentResolver.openInputStream(videoUri)?.use { input ->
                inputVideo.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val outputVideo = File(cacheDir, "output_render_${System.currentTimeMillis()}.mp4")

            // Build filter_complex similar to your Activity version (scale -> pad -> overlay)
            val filter =
                "[0:v]scale=${tw}:${th}[sv];[sv]pad=${blockW}:${blockH}:${tx}:${ty}:color=0x00000000[bg];[bg][1:v]overlay=0:0"

            val cmd = arrayOf(
                "-y",
                "-i", inputVideo.absolutePath,
                "-i", overlayFile.absolutePath,
                "-filter_complex", filter,
                "-c:v", "libx264",
                "-preset", "ultrafast",
                "-crf", "20",
                "-c:a", "aac",
                "-movflags", "+faststart",
                outputVideo.absolutePath
            )

            // Setup log callback for debugging (optional)
            FFmpegKitConfig.enableLogCallback(LogCallback { log ->
                Log.d(TAG, "FFmpegLog: ${log.level} ${log.message}")
            })

            // Setup statistics callback to update notification progress
            FFmpegKitConfig.enableStatisticsCallback(StatisticsCallback { stats: Statistics ->
                // stats.getTime() is milliseconds of processed media
                val timeMs = stats.time
                if (durationMs > 0) {
                    val percent = ((timeMs.toDouble() / durationMs.toDouble()) * 100.0).toInt()
                        .coerceIn(0, 100)
                    // update notification
                    val notif = createNotification(percent)
                    notificationManager.notify(NOTIFICATION_ID, notif)
                }
            })

            // Execute async
            FFmpegKit.executeAsync(cmd.joinToString(" ")) { session ->
                val returnCode = session.returnCode
                if (ReturnCode.isSuccess(returnCode)) {
                    Log.d(TAG, "FFmpeg success: ${outputVideo.absolutePath}")
                    // Save to public storage (MediaStore)
                    val savedUri = saveVideoToPublic(outputVideo)
                    Log.d(TAG, "Saved to MediaStore: $savedUri")

                    // Xử lí video khi mà thành công
                    story?.let { it ->
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                it.durationMs = getVideoDuration(outputVideo)
                                val result = storyRepository.uploadStory(it, outputVideo, true)
                                if (result) {
                                    // --- 1) cập nhật notification FINAL ở đây ---
                                    val finalNotif = NotificationCompat.Builder(
                                        this@VideoRenderService,
                                        CHANNEL_ID
                                    )
                                        .setContentTitle("Tin của bạn đang được đăng")
                                        .setContentText("Đã xử lý xong.")
                                        .setSmallIcon(android.R.drawable.ic_media_play)
                                        .setProgress(0, 0, false)
                                        .build()
                                    notificationManager.notify(NOTIFICATION_ID, finalNotif)

                                    // thành công thì là true thì gửi 1 cái gì đó về bên HomeActivity thì có dc không
                                    val intent = Intent("com.example.easymedia.UPLOAD_DONE")
                                    intent.putExtra(IntentExtras.RESULT_DATA_STR, true)
                                    sendBroadcast(intent)
                                } else {
                                    // nếu như mà không thành công thì sao
                                }
                            } catch (e: Exception) {
                                Log.e("VideoRenderService", "Upload failed", e)
                                // tương tự: notify lỗi + broadcast false
                                val errNotif =
                                    NotificationCompat.Builder(this@VideoRenderService, CHANNEL_ID)
                                        .setContentTitle("Lỗi khi xử lý video")
                                        .setContentText(e.message ?: "Unknown")
                                        .setSmallIcon(android.R.drawable.stat_notify_error)
                                        .setProgress(0, 0, false)
                                        .build()
                                notificationManager.notify(NOTIFICATION_ID, errNotif)

                            }
                        }
                    }
                } else {
                    Log.e(TAG, "FFmpeg failed: $returnCode")
                    Log.e(TAG, session.allLogsAsString)

                    val errNotif = NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Lỗi khi xử lý video")
                        .setContentText("Vui lòng thử lại.")
                        .setSmallIcon(android.R.drawable.stat_notify_error)
                        .setProgress(0, 0, false)
                        .build()
                    notificationManager.notify(NOTIFICATION_ID, errNotif)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception renderVideo", e)
            val errNotif = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Lỗi khi xử lý video")
                .setContentText(e.message ?: "Unknown")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .build()
            notificationManager.notify(NOTIFICATION_ID, errNotif)
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
