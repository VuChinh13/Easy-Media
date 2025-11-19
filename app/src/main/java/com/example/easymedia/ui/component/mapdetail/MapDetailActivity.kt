package com.example.easymedia.ui.component.mapdetail

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.easymedia.R
import com.example.easymedia.databinding.ActivityMapDetailBinding
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.map.display.TomTomMap
import com.tomtom.sdk.map.display.camera.CameraOptions
import com.tomtom.sdk.map.display.image.ImageFactory
import com.tomtom.sdk.map.display.marker.MarkerOptions
import com.tomtom.sdk.map.display.ui.MapFragment
import com.tomtom.sdk.map.display.ui.MapReadyCallback


class MapDetailActivity : AppCompatActivity(), MapReadyCallback {
    private lateinit var tomTomMap: TomTomMap
    private lateinit var binding: ActivityMapDetailBinding
    private var lat = 0.0
    private var lng = 0.0
    private var name = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Nhận dữ liệu từ Intent
        lat = intent.getDoubleExtra("lat", 0.0)
        lng = intent.getDoubleExtra("lng", 0.0)
        name = intent.getStringExtra("name") ?: ""

        // Load MapFragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_container) as MapFragment

        mapFragment.getMapAsync(this)

        binding.btnClose.setOnClickListener {
            finish()
        }

    }

    override fun onMapReady(map: TomTomMap) {
        tomTomMap = map

        val point = GeoPoint(lat, lng)

        // Move camera
        tomTomMap.moveCamera(
            CameraOptions(
                position = point,
                zoom = 15.0
            )
        )

        // Add Marker
        val markerImage = ImageFactory.fromResource(R.drawable.ic_location_pin)

        val markerOptions = MarkerOptions(
            coordinate = point,
            pinImage = markerImage,
            tag = "selected_marker"
        )

        tomTomMap.addMarker(markerOptions)
    }
}
