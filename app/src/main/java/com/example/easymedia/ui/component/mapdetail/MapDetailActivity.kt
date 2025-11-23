package com.example.easymedia.ui.component.mapdetail

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.easymedia.R
import com.example.easymedia.data.model.Post
import com.example.easymedia.databinding.ActivityMapDetailBinding
import com.example.easymedia.ui.component.utils.IntentExtras
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.map.display.TomTomMap
import com.tomtom.sdk.map.display.camera.CameraOptions
import com.tomtom.sdk.map.display.image.ImageFactory
import com.tomtom.sdk.map.display.marker.MarkerOptions
import com.tomtom.sdk.map.display.ui.MapFragment
import com.tomtom.sdk.map.display.ui.MapReadyCallback
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

class MapDetailActivity : AppCompatActivity(), MapReadyCallback {
    private lateinit var tomTomMap: TomTomMap
    private lateinit var binding: ActivityMapDetailBinding

    private var lat = 0.0
    private var lng = 0.0
    private var name = ""
    private var urlImage = ""

    private var markerImage: com.tomtom.sdk.map.display.image.Image? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Nhận dữ liệu Intent
        lat = intent.getDoubleExtra("lat", 0.0)
        lng = intent.getDoubleExtra("lng", 0.0)
        name = intent.getStringExtra("name") ?: ""

        // Ưu tiên lấy ảnh từ Post nếu có
        val post = intent.getParcelableExtra<Post>(IntentExtras.EXTRA_USER)
        urlImage = post?.imageUrls?.firstOrNull() ?: intent.getStringExtra("image").orEmpty()

        Log.d("MapDetail", "URL Image: $urlImage")

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_container) as MapFragment
        mapFragment.getMapAsync(this)

        binding.btnClose.setOnClickListener { finish() }
    }

    override fun onMapReady(map: TomTomMap) {
        tomTomMap = map

        val point = GeoPoint(lat, lng)
        tomTomMap.moveCamera(CameraOptions(position = point, zoom = 15.0))

        if (urlImage.isNotEmpty()) {
            loadCustomMarker(point)
        } else {
            addDefaultMarker(point)
        }

        tomTomMap.addLocationMarkerClickListener { _, mapPosition ->
            tomTomMap.moveCamera(CameraOptions(position = mapPosition, zoom = 18.0))
            true
        }
    }

    private fun loadCustomMarker(point: GeoPoint) {
        Glide.with(this)
            .asBitmap()
            .load(urlImage)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {

                    val resized = resource.scale(dpToPx(70), dpToPx(100))
                    val rounded = createRoundedBitmap(resized, 20f)

                    markerImage = ImageFactory.fromBitmap(rounded)

                    tomTomMap.addMarker(
                        MarkerOptions(
                            coordinate = point,
                            pinImage = markerImage!!,
                            tag = "selected_marker"
                        )
                    )
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun addDefaultMarker(point: GeoPoint) {
        markerImage = ImageFactory.fromResource(R.drawable.ic_location_pin)
        tomTomMap.addMarker(
            MarkerOptions(
                coordinate = point,
                pinImage = markerImage!!,
                tag = "selected_marker"
            )
        )
    }

    private fun createRoundedBitmap(bitmap: Bitmap, radius: Float): Bitmap {
        val output = createBitmap(bitmap.width, bitmap.height)
        val canvas = Canvas(output)

        val paint = Paint().apply {
            isAntiAlias = true
            shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }

        val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        canvas.drawRoundRect(rect, radius, radius, paint)

        return output
    }

    private fun dpToPx(dp: Int): Int {
        val density = Resources.getSystem().displayMetrics.density
        return (dp * density).toInt()
    }
}
