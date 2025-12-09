package com.example.wastesegregationapp

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

/**
 * Simple log entry for when a bin becomes full
 */
data class BinLog(
    val id: String = UUID.randomUUID().toString(),
    val binNumber: Int,
    val binName: String,
    val percentage: Int,
    val timestamp: Long,
    val dateTime: String
)

/**
 * Manages bin logs using SharedPreferences (simple and reliable)
 */
class BinLogManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("BinLogs", Context.MODE_PRIVATE)
    private val gson = Gson()

    /**
     * Save a new log entry
     */
    fun addLog(binNumber: Int, binName: String, percentage: Int) {
        val logs = getAllLogs().toMutableList()

        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        val newLog = BinLog(
            binNumber = binNumber,
            binName = binName,
            percentage = percentage,
            timestamp = System.currentTimeMillis(),
            dateTime = dateFormat.format(Date())
        )

        logs.add(0, newLog) // Add to beginning (newest first)
        saveLogs(logs)
    }

    /**
     * Get all logs (newest first)
     */
    fun getAllLogs(): List<BinLog> {
        val json = prefs.getString("logs", "[]") ?: "[]"
        val type = object : TypeToken<List<BinLog>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    /**
     * Get logs for a specific bin
     */
    fun getLogsForBin(binNumber: Int): List<BinLog> {
        return getAllLogs().filter { it.binNumber == binNumber }
    }

    /**
     * Delete all logs
     */
    fun clearAllLogs() {
        prefs.edit().clear().apply()
    }

    /**
     * Get total log count
     */
    fun getLogCount(): Int {
        return getAllLogs().size
    }

    private fun saveLogs(logs: List<BinLog>) {
        val json = gson.toJson(logs)
        prefs.edit().putString("logs", json).apply()
    }
}