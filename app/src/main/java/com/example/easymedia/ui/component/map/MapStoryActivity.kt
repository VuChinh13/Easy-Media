package com.example.easymedia.ui.component.map

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
import com.tomtom.sdk.map.display.marker.MarkerOptions
import com.tomtom.sdk.map.display.ui.MapFragment
import com.tomtom.sdk.map.display.ui.MapReadyCallback
import com.tomtom.sdk.search.SearchCallback
import com.tomtom.sdk.search.SearchOptions
import com.tomtom.sdk.search.SearchResponse
import com.tomtom.sdk.search.autocomplete.AutocompleteCallback
import com.tomtom.sdk.search.autocomplete.AutocompleteOptions
import com.tomtom.sdk.search.autocomplete.AutocompleteResponse
import com.tomtom.sdk.search.common.error.SearchFailure
import com.tomtom.sdk.search.model.result.AutocompleteSegmentBrand
import com.tomtom.sdk.search.model.result.AutocompleteSegmentPlainText
import com.tomtom.sdk.search.model.result.AutocompleteSegmentPoiCategory
import com.tomtom.sdk.search.online.OnlineSearch
import java.util.Locale

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

    // Thêm biến Handler để xử lý delay tìm kiếm (Debounce)
    private val searchHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

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
        // SỬA PHẦN TEXT WATCHER
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Hủy lệnh tìm kiếm trước đó nếu người dùng vẫn đang gõ
                searchRunnable?.let { searchHandler.removeCallbacks(it) }

                // Tạo lệnh tìm kiếm mới sau 500ms
                searchRunnable = Runnable {
                    val text = s.toString()
                    if (text.isNotEmpty()) {
                        callAutocomplete(text)
                    } else {
                        // Nếu xóa hết chữ thì clear list gợi ý
                        adapter.updateData(emptyList())
                    }
                }
                searchHandler.postDelayed(searchRunnable!!, 500) // Delay 500ms
            }
        })
    }

    // --- HÀM QUAN TRỌNG CẦN SỬA ---
    private fun callAutocomplete(text: String) {
        val currentCenter = if (isMapReady) {
            tomTomMap.cameraPosition.position
        } else {
            GeoPoint(21.028511, 105.804817)
        }

        val options = AutocompleteOptions(
            query = text,
            position = currentCenter,
            radius = Distance.meters(20000),
            locale = Locale("vi", "VN"),
            // SỬA Ở ĐÂY: Đổi "VN" thành "VNM"
            countries = setOf("VNM")
        )

        searchApi.autocompleteSearch(options, object : AutocompleteCallback {
            override fun onFailure(failure: SearchFailure) {
                Log.e(tag, "Autocomplete error: $failure")
            }

            override fun onSuccess(result: AutocompleteResponse) {
                Log.d(tag, "Found ${result.results.size} suggestions")
                adapter.updateData(result.results)
            }
        })
    }

    private fun searchByText(text: String) {
        if (!isMapReady) {
            pendingSearchText = text
            return
        }
        pendingSearchText = null

        val currentCenter = tomTomMap.cameraPosition.position

        val options = SearchOptions(
            query = text,
            geoBias = currentCenter,
            limit = 10,
            locale = Locale("vi", "VN"),
            // SỬA Ở ĐÂY: Đổi "VN" thành "VNM"
            countryCodes = setOf("VNM")
        )

        searchApi.search(options, object : SearchCallback {
            override fun onFailure(failure: SearchFailure) {
                Log.e(tag, "Search error: $failure")
            }

            override fun onSuccess(result: SearchResponse) {
                // ... (giữ nguyên logic xử lý marker) ...
                if (result.results.isNotEmpty()) {
                    val firstResult = result.results[0]
                    val pos = firstResult.place.coordinate

                    if (pos != null) {
                        tomTomMap.moveCamera(CameraOptions(position = pos, zoom = 15.0))

                        // Xóa marker cũ (dùng hàm số nhiều removeMarkers)
                        tomTomMap.removeMarkers("current_marker")

                        val markerImage = ImageFactory.fromResource(R.drawable.ic_location_pin)

                        val markerOptions = MarkerOptions(
                            coordinate = pos,
                            pinImage = markerImage,
                            tag = "current_marker"
                        )
                        currentMarker = tomTomMap.addMarker(markerOptions)
                    }
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
