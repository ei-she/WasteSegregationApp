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
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class ReportsFragment : Fragment() {

    private var barChart: BarChart? = null
    private val dbUrl = "https://wise-wastee-default-rtdb.asia-southeast1.firebasedatabase.app"

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
            loadRealFirebaseData(it)
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
        xAxis.setCenterAxisLabels(true)
        xAxis.valueFormatter = IndexAxisValueFormatter(getDynamicDayLabels())
        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = 7f

        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 105f
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = "${value.toInt()}%"
        }
        chart.axisRight.isEnabled = false

        val l = chart.legend
        l.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        l.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        l.orientation = Legend.LegendOrientation.HORIZONTAL
        l.setDrawInside(false)
        l.yOffset = 5f
        l.xOffset = 0f
        l.textSize = 12f
    }

    private fun loadRealFirebaseData(chart: BarChart) {
        val reportsRef = FirebaseDatabase.getInstance(dbUrl).getReference("reports")
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val last7Days = (0..6).map { i ->
            val tempCal = calendar.clone() as Calendar
            tempCal.add(Calendar.DAY_OF_YEAR, -(6 - i))
            sdf.format(tempCal.time)
        }

        reportsRef.get().addOnSuccessListener { snapshot ->
            val entriesNonRes = mutableListOf<BarEntry>()
            val entriesRes = mutableListOf<BarEntry>()
            val entriesRecyc = mutableListOf<BarEntry>()

            for (i in last7Days.indices) {
                val dateKey = last7Days[i]
                val dayData = snapshot.child(dateKey)

                val res = dayData.child("residual_max").getValue(Int::class.java)?.toFloat() ?: 0f
                val nonRes = dayData.child("non_residual_max").getValue(Int::class.java)?.toFloat() ?: 0f
                val rec = dayData.child("recyclable_max").getValue(Int::class.java)?.toFloat() ?: 0f
                entriesNonRes.add(BarEntry(i.toFloat(), nonRes))
                entriesRes.add(BarEntry(i.toFloat(), res))
                entriesRecyc.add(BarEntry(i.toFloat(), rec))
            }

            val set1 = BarDataSet(entriesNonRes, "Non-Residual").apply { color = WASTE_COLORS[0] }
            val set2 = BarDataSet(entriesRes, "Residual").apply { color = WASTE_COLORS[1] }
            val set3 = BarDataSet(entriesRecyc, "Recyclable").apply { color = WASTE_COLORS[2] }
            val data = BarData(set1, set2, set3)
            val groupSpace = 0.16f
            val barSpace = 0.05f
            val barWidth = 0.23f

            data.barWidth = barWidth
            chart.data = data
            chart.groupBars(0f, groupSpace, barSpace)
            chart.xAxis.axisMinimum = 0f
            chart.xAxis.axisMaximum = 0f + chart.barData.getGroupWidth(groupSpace, barSpace) * 7

            chart.invalidate()
        }
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
        val reportsRef = FirebaseDatabase.getInstance(dbUrl).getReference("reports")

        reportsRef.get().addOnSuccessListener { snapshot ->
            val months = listOf("January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December")

            val monthlyData = months.mapIndexed { index, name ->
                val monthNum = String.format("%02d", index + 1)
                var overflowCount = 0

                for (daySnapshot in snapshot.children) {
                    if (daySnapshot.key?.startsWith("$year-$monthNum") == true) {
                        val max = daySnapshot.child("residual_max").getValue(Int::class.java) ?: 0
                        if (max >= 90) overflowCount++
                    }
                }
                MonthlyReport(name, year, overflowCount)
              }

            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = MonthlyReportAdapter(monthlyData)
        }
    }
}