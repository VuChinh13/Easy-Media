package com.example.easymedia.ui.component.map

import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.easymedia.R
import com.tomtom.quantity.Distance
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.map.display.TomTomMap
import com.tomtom.sdk.map.display.camera.CameraOptions
import com.tomtom.sdk.map.display.image.ImageFactory
import com.tomtom.sdk.map.display.marker.Marker
import com.tomtom.sdk.map.display.ui.MapFragment
import com.tomtom.sdk.map.display.ui.MapReadyCallback
import com.tomtom.sdk.map.display.marker.MarkerOptions
import com.tomtom.sdk.search.SearchOptions
import com.tomtom.sdk.search.SearchCallback
import com.tomtom.sdk.map.display.image.Image
import com.tomtom.sdk.search.SearchResponse
import com.tomtom.sdk.search.autocomplete.*
import com.tomtom.sdk.search.common.error.SearchFailure
import com.tomtom.sdk.search.model.result.*
import com.tomtom.sdk.search.online.OnlineSearch

class MapStoryActivity : AppCompatActivity(), MapReadyCallback {

    private lateinit var searchApi: OnlineSearch
    private lateinit var tomTomMap: TomTomMap
    private var isMapReady = false
    private var pendingSearchText: String? = null

    private lateinit var etSearch: EditText
    private lateinit var rvSuggestions: RecyclerView
    private lateinit var adapter: AutocompleteAdapter
    private var currentMarker: Marker? = null

    private val tag = "mapstory"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_story)

        etSearch = findViewById(R.id.et_search)
        rvSuggestions = findViewById(R.id.rv_suggestions)

        adapter = AutocompleteAdapter { selectedResult ->
            // Build query từ tất cả segments
            val queryText = selectedResult.segments.joinToString(" ") { segment ->
                when (segment) {
                    is AutocompleteSegmentPlainText -> segment.plainText
                    is AutocompleteSegmentPoiCategory -> segment.matchedAlternativeName.ifEmpty { segment.poiCategory.name }
                    is AutocompleteSegmentBrand -> segment.brand.name
                    else -> ""
                }
            }.trim()

            if (queryText.isNotEmpty()) {
                searchByText(queryText)
            }
        }

        rvSuggestions.layoutManager = LinearLayoutManager(this)
        rvSuggestions.adapter = adapter

        // MapFragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_container) as MapFragment
        mapFragment.getMapAsync(this)

        // OnlineSearch
        searchApi = OnlineSearch.create(
            context = this,
            apiKey = "h3ch6HjcPyPGwH2oV1pz7e3m0hlEPr8m"
        ) as OnlineSearch

        // EditText listener
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text.isNotEmpty()) callAutocomplete(text)
            }
        })
    }

    private fun callAutocomplete(text: String) {
        val options = AutocompleteOptions(
            query = text,
            position = GeoPoint(21.028511, 105.804817),
            radius = Distance.meters(500000)
        )

        searchApi.autocompleteSearch(options, object : AutocompleteCallback {
            override fun onFailure(failure: SearchFailure) {
                Log.e(tag, "Autocomplete error: $failure")
            }

            override fun onSuccess(result: AutocompleteResponse) {
                adapter.updateData(result.results)
            }
        })
    }

    private fun searchByText(text: String) {
        if (!isMapReady) {
            // Map chưa sẵn sàng, lưu pending search
            pendingSearchText = text
            return
        }
        pendingSearchText = null

        val options = SearchOptions(
            query = text,
            geoBias = GeoPoint(21.028511, 105.804817) // trung tâm Hà Nội
        )

        searchApi.search(options, object : SearchCallback {
            override fun onFailure(failure: SearchFailure) {
                Log.e(tag, "Search error: $failure")
            }

            override fun onSuccess(result: SearchResponse) {
                if (result.results.isNotEmpty()) {
                    val firstResult = result.results[0]
                    // Lấy vị trí: ưu tiên place.coordinate, fallback poi.position
                    val pos = firstResult.place.coordinate

                    if (pos != null) {
                        // Move camera tới vị trí
                        tomTomMap.moveCamera(CameraOptions(position = pos, zoom = 15.0))
                        Log.d(tag, "Move camera to: ${pos.latitude}, ${pos.longitude}")

                        // Xoá marker cũ nếu có
                        currentMarker?.let { tomTomMap.removeMarkers(it.tag) }

                        val markerImage = ImageFactory.fromResource(R.drawable.ic_location_pin)

                        // Tạo marker đỏ mới
                        val markerOptions = MarkerOptions(
                            coordinate = pos,
                            pinImage = markerImage, // đây mới là hình marker mặc định đỏ
                            tag = "current_marker"
                        )

                        // Thêm marker vào map
                        currentMarker = tomTomMap.addMarker(markerOptions)
                    } else {
                        Log.e(tag, "Không lấy được vị trí từ SearchResult")
                    }
                } else {
                    Log.e(tag, "SearchResponse không có kết quả")
                }
            }
        })
    }

    override fun onMapReady(map: TomTomMap) {
        tomTomMap = map
        isMapReady = true

        // Move camera tới trung tâm Hà Nội
        tomTomMap.moveCamera(CameraOptions(position = GeoPoint(21.028511, 105.804817), zoom = 10.0))

        // Nếu có pending search → chạy
        pendingSearchText?.let { searchByText(it) }
    }
}
