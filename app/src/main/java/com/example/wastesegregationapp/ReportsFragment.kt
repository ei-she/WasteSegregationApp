package com.example.wastesegregationapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TextView
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
import java.text.SimpleDateFormat
import java.util.*

class ReportsFragment : Fragment() {

    private var barChart: BarChart? = null // Using nullable to prevent crashes

    private val WASTE_LABELS = listOf("Non-Residual", "Residual", "Recyclable")
    private val WASTE_COLORS = listOf(
        Color.parseColor("#FFC107"), // Yellow
        Color.parseColor("#4CAF50"), // Green
        Color.parseColor("#2196F3")  // Blue
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        barChart = view.findViewById(R.id.dashboard_bar_chart)

        barChart?.let {
            setupBarChartStyle(it)
            loadBarChartData(it)
        }

        val yearSpinner: Spinner = view.findViewById(R.id.year_spinner)
        yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedYear = parent.getItemAtPosition(position).toString()
                setupMonthlyReports(selectedYear)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupBarChartStyle(chart: BarChart) {
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setDrawBarShadow(false)
        chart.setDrawValueAboveBar(true)
        chart.animateY(1000)

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f // Rotate labels to prevent overlap
        xAxis.valueFormatter = IndexAxisValueFormatter(getDynamicDayLabels())

        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(false) // Cleaner look
        leftAxis.axisMinimum = 0f
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = "${value.toInt()}kg"
        }
        chart.axisRight.isEnabled = false

        val l = chart.legend
        l.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        l.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        l.orientation = Legend.LegendOrientation.VERTICAL
        l.setDrawInside(true)
    }

    private fun loadBarChartData(chart: BarChart) {
        val entries = mutableListOf<BarEntry>()

        for (i in 0 until 7) {
            val val1 = (20..50).random().toFloat()
            val val2 = (30..60).random().toFloat()
            val val3 = (15..40).random().toFloat()
            entries.add(BarEntry(i.toFloat(), floatArrayOf(val1, val2, val3)))
        }

        val set = BarDataSet(entries, "")
        set.colors = WASTE_COLORS
        set.stackLabels = WASTE_LABELS.toTypedArray()
        set.setDrawValues(false) // Keeps the UI clean

        val data = BarData(set)
        data.barWidth = 0.5f

        chart.data = data
        chart.setFitBars(true)
        chart.invalidate()
    }

    private fun getDynamicDayLabels(): Array<String> {
        val labels = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        for (i in 0 until 7) {
            val tempCal = calendar.clone() as Calendar
            tempCal.add(Calendar.DAY_OF_YEAR, -(6 - i))
            labels.add(dateFormat.format(tempCal.time))
        }
        return labels.toTypedArray()
    }

    private fun setupMonthlyReports(year: String) {
        val recyclerView: RecyclerView = requireView().findViewById(R.id.monthly_reports_recycler)
        val months = listOf("January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")
        val monthlyData = months.map { MonthlyReport(it, year) }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = MonthlyReportAdapter(monthlyData)
    }
}