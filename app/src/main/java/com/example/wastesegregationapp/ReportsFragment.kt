package com.example.wastesegregationapp

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import org.json.JSONArray
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class ReportsFragment : Fragment() {

    private var barChart: BarChart? = null
    // Update this IP to match your Raspberry Pi
    private val reportsUrl = "http://192.168.0.147/waste_api/get_daily_reports.php"

    private val WASTE_COLORS = listOf(
        Color.parseColor("#4CAF50"), // Green (Bio)
        Color.parseColor("#F44336"), // Red (Non-Bio)
        Color.parseColor("#2196F3")  // Blue (Others)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        barChart = view.findViewById(R.id.dashboard_bar_chart)
        val btnExportPdf: ImageButton = view.findViewById(R.id.btnExportPdf)

        barChart?.let {
            setupBarChartStyle(it)
            fetchLocalReports(it)
        }

        btnExportPdf.setOnClickListener {
            barChart?.let { chart ->
                if (chart.data != null && chart.data.entryCount > 0) {
                    exportChartToPdf(chart)
                } else {
                    Toast.makeText(requireContext(), "No data to export", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val yearSpinner: Spinner = view.findViewById(R.id.year_spinner)
        yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                barChart?.let { fetchLocalReports(it) }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupBarChartStyle(chart: BarChart) {
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setDrawBarShadow(false)
        chart.animateY(1000)

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.setCenterAxisLabels(true)

        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = 7f

        chart.axisLeft.axisMinimum = 0f
        chart.axisRight.isEnabled = false
        chart.legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        chart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
    }

    private fun fetchLocalReports(chart: BarChart) {
        val queue = Volley.newRequestQueue(requireContext())

        val request = JsonArrayRequest(Request.Method.GET, reportsUrl, null,
            { response ->
                updateChartData(chart, response)
            },
            { error ->
                Log.e("REPORTS_ERR", "Volley Error: ${error.message}")
            }
        )
        queue.add(request)
    }

    private fun updateChartData(chart: BarChart, response: JSONArray) {
        val entriesBio = mutableListOf<BarEntry>()
        val entriesNonBio = mutableListOf<BarEntry>()
        val entriesOthers = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        if (response.length() == 0) {
            chart.clear()
            chart.setNoDataText("No waste data recorded for this week yet.")
            chart.invalidate()
            return
        }

        for (i in 0 until response.length()) {
            val obj = response.getJSONObject(i)
            val dateStr = obj.optString("report_date", "2026-01-01")

            val outFormat = SimpleDateFormat("MM/dd", Locale.US)
            val inFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = inFormat.parse(dateStr) ?: Date()
            labels.add(outFormat.format(date))

            entriesBio.add(BarEntry(i.toFloat(), obj.optInt("bio_count", 0).toFloat()))
            entriesNonBio.add(BarEntry(i.toFloat(), obj.optInt("non_bio_count", 0).toFloat()))
            entriesOthers.add(BarEntry(i.toFloat(), obj.optInt("others_count", 0).toFloat()))
        }

        val set1 = BarDataSet(entriesBio, "Bio").apply { color = WASTE_COLORS[0]; valueTextColor = Color.BLACK; valueTextSize = 10f }
        val set2 = BarDataSet(entriesNonBio, "Non-Bio").apply { color = WASTE_COLORS[1]; valueTextColor = Color.BLACK; valueTextSize = 10f }
        val set3 = BarDataSet(entriesOthers, "Others").apply { color = WASTE_COLORS[2]; valueTextColor = Color.BLACK; valueTextSize = 10f }

        val data = BarData(set1, set2, set3)

        val groupSpace = 0.08f
        val barSpace = 0.03f
        val barWidth = 0.25f

        data.barWidth = barWidth
        chart.data = data

        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.xAxis.axisMinimum = 0f
        chart.xAxis.axisMaximum = 0f + chart.barData.getGroupWidth(groupSpace, barSpace) * labels.size

        chart.groupBars(0f, groupSpace, barSpace)
        chart.setFitBars(true)
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    private fun exportChartToPdf(chart: BarChart) {
        val bitmap = chart.chartBitmap
        val pdfDocument = PdfDocument()
        
        // PDF page size (A4 is roughly 595x842 points)
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width + 40, bitmap.height + 100, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        paint.color = Color.BLACK
        paint.textSize = 20f
        paint.isFakeBoldText = true

        // Draw Title
        canvas.drawText("Waste Analytics Report", 20f, 40f, paint)
        
        // Draw Timestamp
        paint.textSize = 12f
        paint.isFakeBoldText = false
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $timeStamp", 20f, 65f, paint)

        // Draw Chart Bitmap
        canvas.drawBitmap(bitmap, 20f, 80f, null)

        pdfDocument.finishPage(page)

        val fileName = "WasteReport_${System.currentTimeMillis()}.pdf"
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = requireContext().contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    val outputStream: OutputStream? = resolver.openOutputStream(it)
                    outputStream?.use { os ->
                        pdfDocument.writeTo(os)
                    }
                    Toast.makeText(requireContext(), "PDF saved to Downloads", Toast.LENGTH_LONG).show()
                }
            } else {
                // For older versions, would need storage permissions
                Toast.makeText(requireContext(), "Export failed: Version not supported", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("PDF_EXPORT", "Error: ${e.message}")
            Toast.makeText(requireContext(), "Error exporting PDF", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}