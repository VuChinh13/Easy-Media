package com.example.easymedia.ui.component.map

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.easymedia.databinding.ActivityMapStoryBinding
import com.example.easymedia.ui.component.mapdetail.MapDetailActivity
import com.example.easymedia.ui.utils.IntentExtras
import com.tomtom.quantity.Distance
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.search.SearchCallback
import com.tomtom.sdk.search.SearchOptions
import com.tomtom.sdk.search.SearchResponse
import com.tomtom.sdk.search.autocomplete.AutocompleteCallback
import com.tomtom.sdk.search.autocomplete.AutocompleteOptions
import com.tomtom.sdk.search.common.error.SearchFailure
import com.tomtom.sdk.search.online.OnlineSearch
import java.util.Locale

class MapStoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMapStoryBinding
    private lateinit var searchApi: OnlineSearch
    private lateinit var adapter: AutocompleteAdapter
    private var location: com.example.easymedia.data.model.Location? = null
    private val tag = "mapstory"
    private val searchHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = AutocompleteAdapter { item -> handleItemClick(item) }
        binding.rvSuggestions.layoutManager = LinearLayoutManager(this)
        binding.rvSuggestions.adapter = adapter

        // Load search API
        searchApi = OnlineSearch.create(
            context = this,
            apiKey = "h3ch6HjcPyPGwH2oV1pz7e3m0hlEPr8m"
        ) as OnlineSearch

        loadHistoryAndUpdateAdapter()

        // Text watcher
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    val text = s?.toString() ?: ""
                    if (text.isNotEmpty()) callAutocomplete(text)
                    else loadHistoryAndUpdateAdapter() // show history khi xóa text
                }
                searchHandler.postDelayed(searchRunnable!!, 500)
            }
        })

        binding.btnNext.setOnClickListener {
            if (location == null) {
                Toast.makeText(
                    this@MapStoryActivity,
                    "Hãy chọn 1 địa điểm trong phần gợi ý",
                    Toast.LENGTH_SHORT
                ).show()
            }
            location?.let {
                val resultIntent = Intent().apply {
                    putExtra(IntentExtras.RESULT_DATA, it)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }

        binding.btnClose.setOnClickListener { finish() }
    }

    private fun loadHistoryAndUpdateAdapter() {
        val historyList = HistoryStore.loadHistory(this).take(10)
        val displayItems = historyList.map { DisplayItem.History(it) }
        adapter.updateData(displayItems)
    }

    private fun callAutocomplete(text: String) {
        val options = AutocompleteOptions(
            query = text,
            position = GeoPoint(21.028511, 105.804817),
            radius = Distance.meters(20000),
            locale = Locale("vi", "VN"),
            countries = setOf("VNM")
        )
        searchApi.autocompleteSearch(options, object : AutocompleteCallback {
            override fun onFailure(failure: SearchFailure) {
                Log.e(tag, "Autocomplete error: $failure")
            }

            override fun onSuccess(result: com.tomtom.sdk.search.autocomplete.AutocompleteResponse) {
                val suggestions = result.results.map { DisplayItem.Suggestion(it) }
                runOnUiThread { adapter.updateData(suggestions) }
            }
        })
    }

    private fun handleItemClick(item: DisplayItem) {
        when (item) {
            is DisplayItem.History -> {
                // Open MapDetail with history coords
                val fullAddress = item.item.address
                binding.etSearch.setText(fullAddress)
                binding.etSearch.setSelection(fullAddress.length)
                location = com.example.easymedia.data.model.Location(
                    item.item.lat,
                    item.item.lng,
                    fullAddress
                )

                val intent = Intent(this, MapDetailActivity::class.java)
                intent.putExtra("lat", item.item.lat)
                intent.putExtra("lng", item.item.lng)
                intent.putExtra("name", item.item.address)
                startActivity(intent)

                // Move history to top
                HistoryStore.addOrMoveToTop(this, item.item)
            }

            is DisplayItem.Suggestion -> {
                // Convert suggestion to SearchOptions to get full lat/lng
                val queryText = item.result.segments.joinToString(" ") { segment ->
                    when (segment) {
                        is com.tomtom.sdk.search.model.result.AutocompleteSegmentPlainText -> segment.plainText
                        is com.tomtom.sdk.search.model.result.AutocompleteSegmentPoiCategory -> segment.matchedAlternativeName.ifEmpty { segment.poiCategory.name }
                        is com.tomtom.sdk.search.model.result.AutocompleteSegmentBrand -> segment.brand.name
                        else -> ""
                    }
                }.trim()
                if (queryText.isEmpty()) return

                val searchOptions = SearchOptions(
                    query = queryText,
                    geoBias = GeoPoint(21.028511, 105.804817),
                    limit = 1,
                    locale = Locale("vi", "VN"),
                    countryCodes = setOf("VNM")
                )
                searchApi.search(searchOptions, object : SearchCallback {
                    override fun onFailure(failure: SearchFailure) {
                        Log.e(tag, "Search error for '$queryText': $failure")
                    }

                    override fun onSuccess(result: SearchResponse) {
                        val firstResult = result.results.firstOrNull() ?: return
                        val coord = firstResult.place.coordinate
                        val lat = coord.latitude
                        val lng = coord.longitude
                        val addr = firstResult.place.address
                        var fullAddress =
                            addr?.freeformAddress ?: firstResult.place.name

                        // Loại bỏ postalCode (chuỗi toàn số, từ 3-6 chữ số)
                        fullAddress = fullAddress.split(",")
                            .map { it.trim() }
                            .filter { !it.matches(Regex("^\\d{3,6}$")) }
                            .distinct()
                            .joinToString(", ")

                        location = com.example.easymedia.data.model.Location(lat, lng, fullAddress)

                        // Open MapDetail
                        runOnUiThread {
                            binding.etSearch.setText(fullAddress)
                            binding.etSearch.setSelection(fullAddress.length) // đặt con trỏ cuối text
                            val intent =
                                Intent(this@MapStoryActivity, MapDetailActivity::class.java)
                            intent.putExtra("lat", lat)
                            intent.putExtra("lng", lng)
                            intent.putExtra("name", fullAddress)
                            startActivity(intent)
                        }

                        // Save to history
                        val newHistory = HistoryItem(fullAddress, lat, lng)
                        HistoryStore.addOrMoveToTop(this@MapStoryActivity, newHistory)
                    }
                })
            }
        }
    }
}


// file: HistoryItem.kt (hoặc đặt trong cùng file nếu bạn muốn)
data class HistoryItem(
    val address: String,
    val lat: Double,
    val lng: Double,
    val timestamp: Long = System.currentTimeMillis()
)

// Wrapper để adapter có thể hiển thị cả suggestion và history
sealed class DisplayItem {
    data class Suggestion(val result: com.tomtom.sdk.search.model.result.AutocompleteResult) :
        DisplayItem()

    data class History(val item: HistoryItem) : DisplayItem()
}
