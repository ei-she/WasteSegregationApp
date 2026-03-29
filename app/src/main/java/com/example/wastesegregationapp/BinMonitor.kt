package com.example.wastesegregationapp

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class BinMonitoringService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastLevelRes = -1
    private var lastLevelNonRes = -1
    private var lastLevelRecyc = -1

    // Poll every 5 seconds (Fast enough for defense, slow enough for battery)
    private val pollingInterval = 5000L

    private val monitorRunnable = object : Runnable {
        override fun run() {
            fetchBinLevels()
            handler.postDelayed(this, pollingInterval)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.post(monitorRunnable)
        return START_STICKY
    }

    private fun fetchBinLevels() {
        val queue = Volley.newRequestQueue(this)
        // This URL points to your Pi (from config.kt)
        val url = config.BASE_URL + "get_bin_status.php"

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    checkBin("Residual", json.getInt("residual"), 101)
                    checkBin("Non-Residual", json.getInt("non_residual"), 102)
                    checkBin("Recyclable", json.getInt("recyclable"), 103)
                } catch (e: Exception) {
                    Log.e("BinMonitor", "JSON Parsing error: ${e.message}")
                }
            },
            { error -> Log.e("BinMonitor", "Server unreachable: ${error.message}") }
        )
        queue.add(stringRequest)
    }

    private fun checkBin(type: String, currentLevel: Int, id: Int) {
        val lastLevel = when(type) {
            "Residual" -> lastLevelRes
            "Non-Residual" -> lastLevelNonRes
            else -> lastLevelRecyc
        }

        // Trigger notification if bin crosses 90%
        if (currentLevel >= 90 && lastLevel < 90) {
            sendNotification("$type Bin Full!", currentLevel, id)
        }

        // Update trackers
        when(type) {
            "Residual" -> lastLevelRes = currentLevel
            "Non-Residual" -> lastLevelNonRes = currentLevel
            "Recyclable" -> lastLevelRecyc = currentLevel
        }
    }

    // Keep your existing sendNotification function here...
    @android.annotation.SuppressLint("MissingPermission")
    private fun sendNotification(title: String, level: Int, notificationId: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(this, notificationId, intent, android.app.PendingIntent.FLAG_IMMUTABLE)

        val builder = androidx.core.app.NotificationCompat.Builder(this, "BIN_FULL_NOTIF")
            .setSmallIcon(R.drawable.alert) // Make sure this icon exists!
            .setContentTitle(title)
            .setContentText("The bin is at $level%. Please empty it.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = androidx.core.app.NotificationManagerCompat.from(this)
        notificationManager.notify(notificationId, builder.build())
    }

    override fun onDestroy() {
        handler.removeCallbacks(monitorRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}