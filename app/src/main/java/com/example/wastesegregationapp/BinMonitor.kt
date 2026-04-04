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

class BinMonitor : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastBio = -1
    private var lastNon = -1
    private var lastMix = -1

    private val monitorRunnable = object : Runnable {
        override fun run() {
            fetchLevels()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.post(monitorRunnable)
        return START_STICKY
    }

    private fun fetchLevels() {
        val queue = Volley.newRequestQueue(this)
        // Ensure config.GET_DATA_URL points to get_bins.php on your Pi
        val url = config.GET_DATA_URL

        val request = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    // Convert String "21" to Int 21
                    val bio = json.optString("Bio", "0").toIntOrNull() ?: 0
                    val non = json.optString("Non", "0").toIntOrNull() ?: 0
                    val mix = json.optString("others", "0").toIntOrNull() ?: 0

                    Log.d("BinMonitor", "Data Received: Bio=$bio, Non=$non, Mix=$mix")

                    // Update tracked levels
                    lastBio = bio
                    lastNon = non
                    lastMix = mix
                } catch (e: Exception) {
                    Log.e("BinMonitor", "Parse Error: ${e.message}")
                }
            },
            { Log.e("BinMonitor", "Network Error") }
        )
        queue.add(request)
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        handler.removeCallbacks(monitorRunnable)
        super.onDestroy()
    }
}