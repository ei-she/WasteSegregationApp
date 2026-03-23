package com.example.wastesegregationapp

import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.database.ServerValue
import java.text.SimpleDateFormat
import java.util.*

class ReportsFragment : Fragment() {

    private var barChart: BarChart? = null
    private val dbUrl = "https://wise-wastee-default-rtdb.asia-southeast1.firebasedatabase.app"

    private val WASTE_COLORS = listOf(
        Color.parseColor("#FFC107"), // Yellow (Non-Res)
        Color.parseColor("#4CAF50"), // Green (Residual)
        Color.parseColor("#2196F3")  // Blue (Recyc)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        barChart = view.findViewById(R.id.dashboard_bar_chart)

        barChart?.let {
            setupBarChartStyle(it)
            sendStaticData()
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

    private fun sendStaticData() {
        val reportsRef = FirebaseDatabase.getInstance(dbUrl).getReference("reports")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }

        // Seeding 6 days ago up to yesterday
        for (i in 0..5) {
            val tempCal = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"))
            tempCal.add(Calendar.DAY_OF_YEAR, -(6 - i))
            val pastDate = sdf.format(tempCal.time)

            reportsRef.child(pastDate).addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    if (!snapshot.exists()) {
                        val staticTime = "23:59:59"
                        val types = listOf("Residual", "Non-Residual", "Recyclable")

                        for (type in types) {
                            val dummyData = mapOf(
                                "binType" to type,
                                "fillLevel" to (20..65).random().toFloat(),
                                "timestamp" to ServerValue.TIMESTAMP
                            )
                            reportsRef.child(pastDate).child("${staticTime}_$type").setValue(dummyData)
                        }
                    }
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
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
        leftAxis.axisMaximum = 110f
        leftAxis.setLabelCount(6, true)
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
        l.textSize = 10f
    }

    private fun loadRealFirebaseData(chart: BarChart) {
        val reportsRef = FirebaseDatabase.getInstance(dbUrl).getReference("reports")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }

        val last7Days = (0..6).map { i ->
            val tempCal = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"))
            tempCal.add(Calendar.DAY_OF_YEAR, -(6 - i))
            sdf.format(tempCal.time)
        }

        reportsRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val entriesNonRes = mutableListOf<BarEntry>()
                val entriesRes = mutableListOf<BarEntry>()
                val entriesRecyc = mutableListOf<BarEntry>()

                for (i in last7Days.indices) {
                    val targetDate = last7Days[i]
                    var latestRes = 0f
                    var latestNonRes = 0f
                    var latestRecyc = 0f

                    val dayFolder = snapshot.child(targetDate)
                    if (dayFolder.exists()) {
                        for (reportSnapshot in dayFolder.children) {
                            val binType = reportSnapshot.child("binType").getValue(String::class.java) ?: ""
                            val level = reportSnapshot.child("fillLevel").getValue(Float::class.java) ?: 0f

                            when (binType) {
                                "Residual" -> latestRes = level
                                "Non-Residual" -> latestNonRes = level
                                "Recyclable" -> latestRecyc = level
                            }
                        }
                    }

                    entriesNonRes.add(BarEntry(i.toFloat(), latestNonRes))
                    entriesRes.add(BarEntry(i.toFloat(), latestRes))
                    entriesRecyc.add(BarEntry(i.toFloat(), latestRecyc))
                }

                val set1 = BarDataSet(entriesNonRes, "Non-Res").apply { color = WASTE_COLORS[0]; setDrawValues(true) }
                val set2 = BarDataSet(entriesRes, "Residual").apply { color = WASTE_COLORS[1]; setDrawValues(true) }
                val set3 = BarDataSet(entriesRecyc, "Recyc").apply { color = WASTE_COLORS[2]; setDrawValues(true) }

                val barValueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = if (value > 0) "${value.toInt()}%" else ""
                }
                set1.valueFormatter = barValueFormatter
                set2.valueFormatter = barValueFormatter
                set3.valueFormatter = barValueFormatter

                val data = BarData(set1, set2, set3)
                data.barWidth = 0.20f
                chart.data = data
                chart.groupBars(0f, 0.31f, 0.03f)
                chart.notifyDataSetChanged()
                chart.invalidate()
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    private fun getDynamicDayLabels(): Array<String> {
        val labels = mutableListOf<String>()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"))
        val dateFormat = SimpleDateFormat("MM/dd", Locale.US)

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

        reportsRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val months = listOf("January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December")

                val monthlyData = months.mapIndexed { index, name ->
                    val monthNum = String.format("%02d", index + 1)
                    var overflowCount = 0

                    for (dateSnapshot in snapshot.children) {
                        val dateKey = dateSnapshot.key ?: ""
                        if (dateKey.startsWith("$year-$monthNum")) {
                            var dayHadOverflow = false
                            for (report in dateSnapshot.children) {
                                val level = report.child("fillLevel").getValue(Int::class.java) ?: 0
                                if (level >= 90) dayHadOverflow = true
                            }
                            if (dayHadOverflow) overflowCount++
                        }
                    }
                    MonthlyReport(name, year, overflowCount)
                }

                recyclerView.layoutManager = LinearLayoutManager(context)
                recyclerView.adapter = MonthlyReportAdapter(monthlyData)
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }
}