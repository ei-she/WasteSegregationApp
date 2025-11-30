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
import org.w3c.dom.Text

class HomeFragment : Fragment() {

    private lateinit var bin1Bar: ProgressBar
    private lateinit var bin2Bar: ProgressBar
    private lateinit var bin3Bar: ProgressBar
    private lateinit var bin4Bar: ProgressBar
    private lateinit var logoutButton: Button
    private lateinit var warningText: TextView
    private lateinit var handler: Handler

    private lateinit var barChart: BarChart
    private val espUrl = "http://192.168.2.111/data"

    private val client = OkHttpClient()

    private val updateInterval = 3000L

    private val WASTE_LABELS = listOf("Plastic","Biodegradable","Metal","Plastic Bottles")
    private val WASTE_COLORS = listOf(
        Color.parseColor("#FFC107"), // Yellow
        Color.parseColor("#4CAF50"), // Green
        Color.parseColor("#9E9E9E"), // Gray
        Color.parseColor("#2196F3")  // Blue
    )
    private val DEFAULT_YEAR = "2025"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bin1Bar = view.findViewById(R.id.bin1Bar)
        bin2Bar = view.findViewById(R.id.bin2Bar)
        bin3Bar = view.findViewById(R.id.bin3Bar)
        bin4Bar = view.findViewById(R.id.bin4Bar)
        warningText = view.findViewById(R.id.warningText)
        logoutButton = view.findViewById(R.id.buttonLogout)

        logoutButton.setOnClickListener {
            (activity as? MainActivity)?.logoutUser()

        }
        handler = Handler(Looper.getMainLooper())

        barChart = view.findViewById(R.id.dashboard_bar_chart)

        setupBarChartStyle()
        startAutoUpdate()

        viewLifecycleOwner.lifecycleScope.launch {
            val wasteData = withContext(Dispatchers.Default) {
                getWasteData(DEFAULT_YEAR)
            }
            loadBarChartData(wasteData)
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
}