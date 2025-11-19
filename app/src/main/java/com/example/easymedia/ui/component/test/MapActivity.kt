package com.example.easymedia.ui.component.test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.easymedia.R
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.map.display.MapOptions
import com.tomtom.sdk.map.display.TomTomMap
import com.tomtom.sdk.map.display.ui.MapFragment
import com.tomtom.sdk.map.display.ui.MapReadyCallback


class MapActivity : AppCompatActivity(), MapReadyCallback {

    private lateinit var tomTomMap: TomTomMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Tạo MapOptions với API Key
        val mapOptions = MapOptions(mapKey = "h3ch6HjcPyPGwH2oV1pz7e3m0hlEPr8m")

        // Tạo instance MapFragment
        val mapFragment = MapFragment.newInstance(mapOptions)

        // Add fragment vào container
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commit()

        // Khi map sẵn sàng thì callback gọi onMapReady
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: TomTomMap) {
        tomTomMap = map

        val cameraOptions = com.tomtom.sdk.map.display.camera.CameraOptions(
            position = GeoPoint(21.0278, 105.8342),
            zoom = 10.0,
            tilt = 0.0,
            rotation = 0.0
        )

        tomTomMap.moveCamera(cameraOptions)
    }


    override fun onStart() {
        super.onStart()
        // Nếu MapView cần lifecycle, nhưng dùng MapFragment thì TomTom quản lý phần lớn
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup nếu cần
    }
}
