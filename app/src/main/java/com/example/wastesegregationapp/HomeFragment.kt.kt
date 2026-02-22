
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
import androidx.fragment.app.activityViewModels
import org.w3c.dom.Text

class HomeFragment : Fragment() {

    private lateinit var bin1Bar: ProgressBar
    private lateinit var bin2Bar: ProgressBar
    private lateinit var bin3Bar: ProgressBar
    private lateinit var logoutButton: Button
    private lateinit var warningText: TextView
    private lateinit var handler: Handler
    private lateinit var tipText: TextView
    private lateinit var barChart: BarChart
    private val espUrl = "http://192.168.2.111/data"

    private val segregationTips = listOf(
        "Rinse plastic containers before throwing them in the Recyclable bin.",
        "Food-stained paper (like pizza boxes) belongs in Residual waste.",
        "Crush plastic bottles and tin cans to save space in your bins.",
        "Biodegradable waste can be used for composting your garden!",
        "Keep recyclables dry. Wet paper can ruin a whole batch of recycling.",
        "Check for the recycling symbol on plastics to sort them correctly.",
        "Batteries and electronics are hazardous; don't put them in regular bins!"
    )
//    private val client = OkHttpClient()
//
//    private val updateInterval = 3000L
//
//    private val viewModel: BinDataViewModel by activityViewModels()
//
//    private val WASTE_LABELS = listOf("Non-Residual","Residual","Recyclable")
//    private val WASTE_COLORS = listOf(
//        Color.parseColor("#FFC107"), // Yellow
//        Color.parseColor("#4CAF50"), // Green
//        Color.parseColor("#2196F3")  // Blue
//    )
    private val DEFAULT_YEAR = "2026"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

//    private fun startAutoUpdate() {
//        handler.post(object : Runnable {
//            override fun run() {
//                fetchData()
//                handler.postDelayed(this, updateInterval)
//            }
//        })
//    }
//
//    private fun fetchData() {
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val request = Request.Builder().url(espUrl).build()
//                val response = client.newCall(request).execute()
//                val responseBody = response.body?.string()
//
//                if (response.isSuccessful && responseBody != null) {
//                    val JSON = JSONObject(responseBody)
//                    val bin1 = json.getInt("bin1")
//                    val bin2 = json.getInt("bin2")
//                    val bin3 = json.getInt("bin3")
//
//                    withContext(Dispatchers.Main) {
//                        updateUI(bin1, bin2, bin3)
//                    }
//                } else {
//                    Log.e("HomeFragment", "Failed response: ${response.code}")
//                }
//            } catch (e: Exception) {
//                Log.e("HomeFragment", "Error fetching data", e)
//            }
//        }
//    }
//

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bin1Bar = view.findViewById(R.id.bin1Bar)
        bin2Bar = view.findViewById(R.id.bin2Bar)
        bin3Bar = view.findViewById(R.id.bin3Bar)
        warningText = view.findViewById(R.id.warningText)
        logoutButton = view.findViewById(R.id.buttonLogout)

        logoutButton.setOnClickListener {
            (activity as? MainActivity)?.logoutUser()

        }
        showRandomTip()

        handler = Handler(Looper.getMainLooper())
        //barChart = view.findViewById(R.id.dashboard_bar_chart)

//        setupBarChartStyle()
        startAutoUpdate()
//
//        viewLifecycleOwner.lifecycleScope.launch {
//            val wasteData = withContext(Dispatchers.Default) {
//                getWasteData(DEFAULT_YEAR)
//            }
//            loadBarChartData(wasteData)
//        }

    }

    private fun simulateLiveData() {
    val mockBin1 = (40..50).random()
    val mockBin2 = (15..25).random()
    val mockBin3 = (75..85).random()

    updateUI(mockBin1, mockBin2, mockBin3)
}
    private fun showRandomTip() {
        if (::tipText.isInitialized) {
            tipText.text = segregationTips.random()
        }
    }

    private fun updateUI(bin1: Int, bin2: Int, bin3: Int) {
        try {
            if (!isAdded || view == null) return

            view?.findViewById<ProgressBar>(R.id.bin1Bar)?.progress = bin1
            view?.findViewById<ProgressBar>(R.id.bin2Bar)?.progress = bin2
            view?.findViewById<ProgressBar>(R.id.bin3Bar)?.progress = bin3

            val warnings = StringBuilder()

            if (bin1 >= 80) warnings.append("⚠️ Residual Bin is getting full\n")
            if (bin2 >= 80) warnings.append("⚠️ Non-Residual Bin is getting full\n")
            if (bin3 >= 80) warnings.append("⚠️ Recyclable Bin is getting full\n")

            val warningLabel = view?.findViewById<TextView>(R.id.warningText)

            if (warnings.isNotEmpty()) {
                warningLabel?.visibility = View.VISIBLE
                warningLabel?.text = warnings.toString().trim()
                warningLabel?.setBackgroundColor(Color.parseColor("#FFF59D"))
            } else {
                warningLabel?.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e("HomeError", "UpdateUI failed: ${e.message}")
        }
    }

    private fun startAutoUpdate() {
        handler.removeCallbacksAndMessages(null)
        val runnable = object : Runnable {
            override fun run() {
                if (isAdded && view != null) {
                    // 1. Refresh Tip
                    val tipLabel = view?.findViewById<TextView>(R.id.textSegregationTip)
                    tipLabel?.text = segregationTips.random()

                    // 2. Refresh Bins with Random Numbers (to prove it's live)
                    val r1 = (10..95).random()
                    val r2 = (10..95).random()
                    val r3 = (10..95).random()
                    updateUI(r1, r2, r3)

                    handler.postDelayed(this, 4000L) // 4 seconds
                }
            }
        }
        handler.post(runnable)
    }


//    private fun setupBarChartStyle() {
//        barChart.description.isEnabled = false
//        barChart.setDrawGridBackground(false)
//        barChart.animateY(1000)
//
//        barChart.setTouchEnabled(false)
//        barChart.setPinchZoom(false)
//        barChart.setDragEnabled(false)
//        barChart.setHighlightPerTapEnabled(false)
//        barChart.setHighlightPerDragEnabled(false)
//
//        val leftAxis = barChart.axisLeft
//        leftAxis.axisMinimum = 0f
//        leftAxis.axisMaximum = 500f
//        leftAxis.granularity = 100f
//        leftAxis.valueFormatter = object : ValueFormatter() {
//            override fun getFormattedValue(value: Float): String {
//                return "${value.toInt()}KG"
//            }
//        }
//        barChart.axisRight.isEnabled = false
//
//
//        val months = getMonthLabels()
//        val xAxis = barChart.xAxis
//        xAxis.position = XAxis.XAxisPosition.BOTTOM
//        xAxis.setDrawGridLines(false)
//        xAxis.granularity = 1f
//        xAxis.isGranularityEnabled = true
//        xAxis.labelCount = months.size
//        xAxis.valueFormatter = IndexAxisValueFormatter(months.toTypedArray())
//
//        val legend = barChart.legend
//        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
//        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER)
//        legend.orientation = Legend.LegendOrientation.HORIZONTAL
//        legend.setDrawInside(false)
//        legend.xEntrySpace = 15f
//        legend.formSize = 8f
//        legend.form = Legend.LegendForm.SQUARE
//    }


//    private fun loadBarChartData(wasteData: List<Pair<String, List<Float>>>) {
//        val barEntries = mutableListOf<BarEntry>()
//        val groupCount = wasteData.size
//
//        for (i in 0 until groupCount) {
//            val dataValues = wasteData[i].second.toFloatArray()
//            barEntries.add(BarEntry(i.toFloat(), dataValues))
//        }
//
//        val set = BarDataSet(barEntries, "")
//        set.colors = WASTE_COLORS
//        set.stackLabels = WASTE_LABELS.toTypedArray()
//        val data = BarData(set)
//        data.barWidth = 0.7f
//
//        barChart.data = data
//        barChart.setFitBars(true)
//        barChart.invalidate()
//    }

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
}

