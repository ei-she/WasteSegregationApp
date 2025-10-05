package com.example.wastesegregationapp


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast

class MonthlyReportAdapter(private val reports: List<MonthlyReport>) :
    RecyclerView.Adapter<MonthlyReportAdapter.ReportViewHolder>() {

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val monthTextView: TextView = itemView.findViewById(R.id.text_month_name)
        val viewButton: Button = itemView.findViewById(R.id.button_view_report)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monthly_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]

        holder.monthTextView.text = "${report.monthName.uppercase()} ${report.year}"

        holder.viewButton.setOnClickListener {
            // TODO: Implement navigation or action when "VIEW" is clicked
            Toast.makeText(holder.itemView.context, "Viewing report for ${report.monthName}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = reports.size
}