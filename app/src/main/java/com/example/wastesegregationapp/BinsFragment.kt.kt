package com.example.wastesegregationapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BinsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var logAdapter: BinLogAdapter
    private lateinit var logManager: BinLogManager
    private lateinit var titleText: TextView
    private lateinit var btnClearLogs: Button
    private lateinit var emptyMessage: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("BinsFragment", "onCreateView called")
        return inflater.inflate(R.layout.fragment_bins, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("BinsFragment", "onViewCreated called")

        try {
            // Initialize views
            recyclerView = view.findViewById(R.id.recyclerBins)
            titleText = view.findViewById(R.id.titleBins)
            btnClearLogs = view.findViewById(R.id.btnClearLogs)
            emptyMessage = view.findViewById(R.id.emptyMessage)

            // Setup RecyclerView
            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            // Initialize log manager
            logManager = BinLogManager(requireContext())

            // Initialize adapter
            logAdapter = BinLogAdapter(emptyList())
            recyclerView.adapter = logAdapter

            // Setup button
            btnClearLogs.setOnClickListener {
                Log.d("BinsFragment", "Clear button clicked")
                logManager.clearAllLogs()
                loadLogs()
                android.widget.Toast.makeText(
                    requireContext(),
                    "All logs cleared",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }

            // Load logs
            loadLogs()

            Log.d("BinsFragment", "Setup complete")

        } catch (e: Exception) {
            Log.e("BinsFragment", "Error in onViewCreated", e)
            android.widget.Toast.makeText(
                requireContext(),
                "Error loading logs: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("BinsFragment", "onResume called - refreshing logs")
        loadLogs()
    }

    private fun loadLogs() {
        try {
            val logs = logManager.getAllLogs()
            Log.d("BinsFragment", "📊 Loading logs... Found: ${logs.size}")

            if (logs.isEmpty()) {
                Log.w("BinsFragment", "⚠️ No logs found!")
                emptyMessage.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyMessage.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                logs.forEachIndexed { index, log ->
                    Log.d("BinsFragment", "  [$index] ${log.binName} - ${log.dateTime} - ${log.percentage}%")
                }
            }

            logAdapter.updateData(logs)
            titleText.text = "Bin Logs (${logs.size})"

        } catch (e: Exception) {
            Log.e("BinsFragment", "Error loading logs", e)
            titleText.text = "Bin Logs (Error)"
        }
    }
}

/**
 * Adapter for displaying bin logs
 */
class BinLogAdapter(
    private var logs: List<BinLog>
) : RecyclerView.Adapter<BinLogAdapter.LogViewHolder>() {

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binName: TextView = view.findViewById(R.id.logBinName)
        val dateTime: TextView = view.findViewById(R.id.logDateTime)
        val percentage: TextView = view.findViewById(R.id.logPercentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        Log.d("BinLogAdapter", "Creating view holder")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bin_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        Log.d("BinLogAdapter", "Binding position $position: ${log.binName}")

        holder.binName.text = "${log.binName} Bin"
        holder.dateTime.text = log.dateTime
        holder.percentage.text = "${log.percentage}%"
    }

    override fun getItemCount(): Int {
        Log.d("BinLogAdapter", "getItemCount: ${logs.size}")
        return logs.size
    }

    fun updateData(newLogs: List<BinLog>) {
        Log.d("BinLogAdapter", "Updating data with ${newLogs.size} logs")
        logs = newLogs
        notifyDataSetChanged()
    }
}