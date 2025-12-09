package com.example.wastesegregationapp

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Button
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import androidx.lifecycle.lifecycleScope

class HomeFragment : Fragment() {

    private lateinit var bin1Bar: ProgressBar
    private lateinit var bin2Bar: ProgressBar
    private lateinit var bin3Bar: ProgressBar
    private lateinit var bin4Bar: ProgressBar
    private lateinit var logoutButton: Button
    private lateinit var warningText: TextView
    private lateinit var handler: Handler
    private lateinit var barChart: BarChart

    // Log manager
    private lateinit var logManager: BinLogManager

    // Tracking bin states for logging
    private val binStates = mutableMapOf<Int, BinState>()

    private val espUrl = "http://192.168.100.37/data"
    private val client = OkHttpClient()
    private val updateInterval = 3000L

    private val WASTE_LABELS = listOf("Plastic", "Biodegradable", "Metal", "Plastic Bottles")
    private val WASTE_COLORS = listOf(
        Color.parseColor("#FFC107"),
        Color.parseColor("#4CAF50"),
        Color.parseColor("#9E9E9E"),
        Color.parseColor("#2196F3")
    )
    private val DEFAULT_YEAR = "2025"

    // Bin configuration
    private val binNames = mapOf(
        1 to "Plastic",
        2 to "Metal",
        3 to "Biodegradable",
        4 to "Plastic Bottles"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize log manager
        logManager = BinLogManager(requireContext())

        // Initialize bin states
        for (i in 1..4) {
            binStates[i] = BinState()
        }

        bin1Bar = view.findViewById(R.id.bin1Bar)
        bin2Bar = view.findViewById(R.id.bin2Bar)
        bin3Bar = view.findViewById(R.id.bin3Bar)
        bin4Bar = view.findViewById(R.id.bin4Bar)
        warningText = view.findViewById(R.id.warningText)
        logoutButton = view.findViewById(R.id.buttonLogout)
        barChart = view.findViewById(R.id.dashboard_bar_chart)
        handler = Handler(Looper.getMainLooper())

        logoutButton.setOnClickListener {
            (activity as? MainActivity)?.logoutUser()
        }

        setupBarChartStyle()
        startAutoUpdate()

        viewLifecycleOwner.lifecycleScope.launch {
            val wasteData = withContext(Dispatchers.Default) {
                getWasteData(DEFAULT_YEAR)
            }
            loadBarChartData(wasteData)
        }
    }

    private fun startAutoUpdate() {
        handler.post(object : Runnable {
            override fun run() {
                fetchData()
                handler.postDelayed(this, updateInterval)
            }
        })
    }

    private fun fetchData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder().url(espUrl).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val json = JSONObject(responseBody)
                    val bin1 = json.getInt("bin1")
                    val bin2 = json.getInt("bin2")
                    val bin3 = json.getInt("bin3")
                    val bin4 = json.getInt("bin4")

                    // Check and log each bin
                    checkAndLogBin(1, bin1)
                    checkAndLogBin(2, bin2)
                    checkAndLogBin(3, bin3)
                    checkAndLogBin(4, bin4)

                    withContext(Dispatchers.Main) {
                        updateUI(bin1, bin2, bin3, bin4)
                    }
                } else {
                    Log.e("HomeFragment", "Failed response: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error fetching data", e)
            }
        }
    }

    /**
     * Check bin state and create log if conditions are met
     */
    private fun checkAndLogBin(binNumber: Int, percentage: Int) {
        val state = binStates[binNumber] ?: return
        val currentTime = System.currentTimeMillis()

        when {
            // Bin reached 95% (FULL state)
            percentage >= 95 -> {
                if (!state.isFullStateReached) {
                    // First time reaching 95%
                    state.isFullStateReached = true
                    state.fullStateStartTime = currentTime
                    Log.d("HomeFragment", "Bin $binNumber reached FULL (95%)")
                } else {
                    // Check if it has stayed at 95% for 10 seconds
                    val duration = currentTime - state.fullStateStartTime
                    if (duration >= 10_000 && !state.hasLoggedFull) {
                        // Log it!
                        val binName = binNames[binNumber] ?: "Unknown"
                        logManager.addLog(binNumber, binName, percentage)
                        state.hasLoggedFull = true
                        // Verify it was saved
                        val totalLogs = logManager.getLogCount()
                        Log.i("HomeFragment", "✅ LOGGED: $binName bin is FULL at $percentage%")
                        Log.i("HomeFragment", "📊 Total logs now: $totalLogs")

                        // Show toast on main thread
                        CoroutineScope(Dispatchers.Main).launch {
                            android.widget.Toast.makeText(
                                requireContext(),
                                "📝 Logged: $binName bin is FULL!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            // Bin went back to empty (below 5%)
            percentage <= 5 -> {
                if (state.hasLoggedFull) {
                    // Reset state - bin was emptied
                    if (!state.isEmptyStateReached) {
                        state.isEmptyStateReached = true
                        state.emptyStateStartTime = currentTime
                    } else {
                        val duration = currentTime - state.emptyStateStartTime
                        if (duration >= 10_000) {
                            // Confirmed empty for 10 seconds, reset everything
                            state.reset()
                            Log.d("HomeFragment", "Bin $binNumber reset to empty state")
                        }
                    }
                }
            }

            // Bin is between 5% and 95%
            else -> {
                // Reset timers if bin moved out of full/empty range
                if (percentage < 95 && state.isFullStateReached && !state.hasLoggedFull) {
                    state.isFullStateReached = false
                }
                if (percentage > 5 && state.isEmptyStateReached) {
                    state.isEmptyStateReached = false
                }
            }
        }
    }

    private fun updateUI(bin1: Int, bin2: Int, bin3: Int, bin4: Int) {
        bin1Bar.progress = bin1
        bin2Bar.progress = bin2
        bin3Bar.progress = bin3
        bin4Bar.progress = bin4

        val warnings = StringBuilder()

        if (bin1 >= 95) warnings.append("⚠️ Bin 1 Full!\n")
        else if (bin1 >= 80) warnings.append("⚠️ Bin 1 Almost Full\n")

        if (bin2 >= 95) warnings.append("⚠️ Bin 2 Full!\n")
        else if (bin2 >= 80) warnings.append("⚠️ Bin 2 Almost Full\n")

        if (bin3 >= 95) warnings.append("⚠️ Bin 3 Full!\n")
        else if (bin3 >= 80) warnings.append("⚠️ Bin 3 Almost Full\n")

        if (bin4 >= 95) warnings.append("⚠️ Bin 4 Full!\n")
        else if (bin4 >= 80) warnings.append("⚠️ Bin 4 Almost Full\n")

        if (warnings.isNotEmpty()) {
            warningText.visibility = View.VISIBLE
            warningText.text = warnings.toString().trim()
            warningText.setBackgroundColor(Color.parseColor("#FFF59D"))
            warningText.setTextColor(Color.BLACK)
        } else {
            warningText.visibility = View.GONE
        }
    }

    private fun setupBarChartStyle() {
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.animateY(1000)
        barChart.setTouchEnabled(false)
        barChart.setPinchZoom(false)
        barChart.setDragEnabled(false)
        barChart.setHighlightPerTapEnabled(false)
        barChart.setHighlightPerDragEnabled(false)

        val leftAxis = barChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 500f
        leftAxis.granularity = 100f
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}KG"
            }
        }
        barChart.axisRight.isEnabled = false

        val months = getMonthLabels()
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true
        xAxis.labelCount = months.size
        xAxis.valueFormatter = IndexAxisValueFormatter(months.toTypedArray())

        val legend = barChart.legend
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER)
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.xEntrySpace = 15f
        legend.formSize = 8f
        legend.form = Legend.LegendForm.SQUARE
    }

    private fun loadBarChartData(wasteData: List<Pair<String, List<Float>>>) {
        val barEntries = mutableListOf<BarEntry>()
        val groupCount = wasteData.size

        for (i in 0 until groupCount) {
            val dataValues = wasteData[i].second.toFloatArray()
            barEntries.add(BarEntry(i.toFloat(), dataValues))
        }

        val set = BarDataSet(barEntries, "")
        set.colors = WASTE_COLORS
        set.stackLabels = WASTE_LABELS.toTypedArray()
        val data = BarData(set)
        data.barWidth = 0.7f

        barChart.data = data
        barChart.setFitBars(true)
        barChart.invalidate()
    }

    private fun getMonthLabels(): List<String> {
        return listOf(
            "Oct 1", "Oct 2", "Oct 3", "Oct 4",
            "Oct 5", "Oct 6", "Oct 7"
        )
    }

    private fun getWasteData(year: String): List<Pair<String, List<Float>>> {
        return getMonthLabels().mapIndexed { index, day ->
            val baseValue = 40f + (index - 3) * 2f
            Pair(
                day,
                listOf(baseValue - 5f, baseValue + 10f, baseValue - 10f, baseValue)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * Tracks the state of a bin for logging purposes
     */
    private data class BinState(
        var isFullStateReached: Boolean = false,
        var fullStateStartTime: Long = 0,
        var hasLoggedFull: Boolean = false,
        var isEmptyStateReached: Boolean = false,
        var emptyStateStartTime: Long = 0
    ) {
        fun reset() {
            isFullStateReached = false
            fullStateStartTime = 0
            hasLoggedFull = false
            isEmptyStateReached = false
            emptyStateStartTime = 0
        }
    }
}