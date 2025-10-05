package com.example.wastesegregationapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter

class ReportsFragment : Fragment() {

    private lateinit var barChart: BarChart

    // Data definitions
    private val WASTE_LABELS = listOf("PLASTIC", "BIODEGRADABLE", "METAL", "PLASTIC BOTTLES")
    private val WASTE_COLORS = listOf(
        Color.parseColor("#FFC107"),
        Color.parseColor("#4CAF50"),
        Color.parseColor("#9E9E9E"),
        Color.parseColor("#2196F3")
    )

    private val DEFAULT_YEAR = "2025"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        barChart = view.findViewById(R.id.waste_bar_chart)
        setupBarChartStyle()

        loadBarChartData(DEFAULT_YEAR)
        setupMonthlyReports(DEFAULT_YEAR)

        val yearSpinner: Spinner = view.findViewById(R.id.year_spinner)
        yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedYear = parent.getItemAtPosition(position).toString()

                loadBarChartData(selectedYear)
                setupMonthlyReports(selectedYear)
            }
            override fun onNothingSelected(parent: AdapterView<*>) { /* No-op */ }
        }
    }

    private fun setupBarChartStyle() {
        // Basic configuration and styling
        barChart.description.isEnabled = false
        barChart.setPinchZoom(false)
        barChart.setDrawGridBackground(false)
        barChart.animateY(1000)

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

    private fun loadBarChartData(year: String) {
        val wasteData = getWasteData(year)
        val barEntries = mutableListOf<BarEntry>()
        val groupCount = wasteData.size

        for (i in 0 until groupCount) {
            val dataValues = wasteData[i].second.toFloatArray()
            barEntries.add(BarEntry(i.toFloat(), dataValues))
        }

        val set = BarDataSet(barEntries, "")
        set.colors = WASTE_COLORS
/*
        set.stackLabels = WASTE_LABELS
*/
        val data = BarData(set)
        data.barWidth = 0.8f

        barChart.data = data
        barChart.setFitBars(true)
        barChart.invalidate()
    }

    // Helper function to get month labels(used by X-Axis setup)
    private fun getMonthLabels(): List<String> {
        return listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
    }

    private fun getWasteData(year: String): List<Pair<String, List<Float>>> {
        // In a real app, this would use the 'year' to fetch specific data.
        return getMonthLabels().mapIndexed { index, month ->
            val baseValue = 400f + (index - 5) * 5 // Subtle variation across months
            Pair(
                month,
                listOf(baseValue - 20f, baseValue + 10f, baseValue - 30f, baseValue)
            )
        }
    }

    private fun setupMonthlyReports(year: String) {
        val recyclerView: RecyclerView = requireView().findViewById(R.id.monthly_reports_recycler)

        val monthlyData = generateMonthlyReports(year)

        val adapter = MonthlyReportAdapter(monthlyData)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun generateMonthlyReports(year: String): List<MonthlyReport> {
        val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        return months.map { month -> MonthlyReport(month, year) }
    }
}