package com.example.easymedia.ui.component.map

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.easymedia.R
import com.example.easymedia.ui.component.mapdetail.MapDetailActivity
import com.tomtom.quantity.Distance
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.search.SearchCallback
import com.tomtom.sdk.search.SearchOptions
import com.tomtom.sdk.search.SearchResponse
import com.tomtom.sdk.search.common.error.SearchFailure
import com.tomtom.sdk.search.model.result.AutocompleteResult
import com.tomtom.sdk.search.model.result.AutocompleteSegmentBrand
import com.tomtom.sdk.search.model.result.AutocompleteSegmentPlainText
import com.tomtom.sdk.search.model.result.AutocompleteSegmentPoiCategory
import com.tomtom.sdk.search.online.OnlineSearch
import com.tomtom.sdk.search.autocomplete.AutocompleteCallback
import com.tomtom.sdk.search.autocomplete.AutocompleteOptions
import com.tomtom.sdk.search.autocomplete.AutocompleteResponse
import java.util.Locale

class MapStoryActivity : AppCompatActivity() {
    private lateinit var searchApi: OnlineSearch
    private lateinit var etSearch: EditText
    private lateinit var rvSuggestions: RecyclerView
    private lateinit var btnFinish: Button
    private lateinit var adapter: AutocompleteAdapter
    private var location = com.example.easymedia.data.model.Location()
    private val tag = "mapstory"
    private val searchHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_story)

        etSearch = findViewById(R.id.et_search)
        rvSuggestions = findViewById(R.id.rv_suggestions)
        btnFinish = findViewById(R.id.btnNext)

        adapter = AutocompleteAdapter { selectedResult ->
            handleSuggestionClick(selectedResult)
        }

        rvSuggestions.layoutManager = LinearLayoutManager(this)
        rvSuggestions.adapter = adapter

        searchApi = OnlineSearch.create(
            context = this,
            apiKey = "h3ch6HjcPyPGwH2oV1pz7e3m0hlEPr8m"
        ) as OnlineSearch

        // Search Watcher (debounce 500ms)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }

                searchRunnable = Runnable {
                    val text = s?.toString() ?: ""
                    if (text.isNotEmpty()) {
                        callAutocomplete(text)
                    } else {
                        adapter.updateData(emptyList())
                    }
                }
                searchHandler.postDelayed(searchRunnable!!, 500)
            }
        })
    }

    private fun callAutocomplete(text: String) {
        val options = AutocompleteOptions(
            query = text,
            position = GeoPoint(21.028511, 105.804817), // bias to Hà Nội
            radius = Distance.meters(20000),
            locale = Locale("vi", "VN"),
            countries = setOf("VNM")
        )

        searchApi.autocompleteSearch(options, object : AutocompleteCallback {
            override fun onFailure(failure: SearchFailure) {
                Log.e(tag, "Autocomplete error: $failure")
            }

            override fun onSuccess(result: AutocompleteResponse) {
                runOnUiThread {
                    adapter.updateData(result.results)
                }
            }
        })
    }

    private fun handleSuggestionClick(result: AutocompleteResult) {

        // Build query từ segments
        val queryText = result.segments.joinToString(" ") { segment ->
            when (segment) {
                is AutocompleteSegmentPlainText -> segment.plainText
                is AutocompleteSegmentPoiCategory -> segment.matchedAlternativeName.ifEmpty { segment.poiCategory.name }
                is AutocompleteSegmentBrand -> segment.brand.name
                else -> ""
            }
        }.trim()

        if (queryText.isEmpty()) return

        // Gọi Search API để lấy full address + tọa độ
        val searchOptions = SearchOptions(
            query = queryText,
            geoBias = GeoPoint(21.028511, 105.804817),
            limit = 5,
            locale = Locale("vi", "VN"),
            countryCodes = setOf("VNM")
        )

        searchApi.search(searchOptions, object : SearchCallback {
            override fun onFailure(failure: SearchFailure) {
                Log.e(tag, "Search error for '$queryText': $failure")
            }

            override fun onSuccess(response: SearchResponse) {

                val firstResult = response.results.firstOrNull() ?: return

                // Lấy tọa độ
                val coord = firstResult.place.coordinate ?: return
                val lat = coord.latitude
                val lng = coord.longitude

                // Lấy full address từ Address
                val addr = firstResult.place.address
                var fullAddress = addr?.freeformAddress ?: firstResult.place.name ?: queryText

                // Loại bỏ postalCode (chuỗi toàn số, từ 3-6 chữ số)
                fullAddress = fullAddress.split(",")
                    .map { it.trim() }
                    .filter { !it.matches(Regex("^\\d{3,6}$")) }
                    .distinct()
                    .joinToString(", ")

                // ✅ Cập nhật EditText
                runOnUiThread {
                    etSearch.setText(fullAddress)
                    etSearch.setSelection(fullAddress.length) // đặt con trỏ cuối text
                }

                btnFinish.visibility = View.VISIBLE

                Log.i("CheckAddress", fullAddress)
                runOnUiThread {
                    val intent = Intent(this@MapStoryActivity, MapDetailActivity::class.java)
                    intent.putExtra("lat", lat)
                    intent.putExtra("lng", lng)
                    intent.putExtra("name", fullAddress)
                    startActivity(intent)
                }
            }
        })
    }
}
