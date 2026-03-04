package com.example.wastesegregationapp

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Button
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import androidx.lifecycle.lifecycleScope

class HomeFragment : Fragment() {

    private lateinit var bin1Bar: ProgressBar
    private lateinit var bin2Bar: ProgressBar
    private lateinit var bin3Bar: ProgressBar
    private lateinit var logoutButton: Button
    private lateinit var warningText: TextView
    private lateinit var handler: Handler
    private lateinit var tipText: TextView
    private val espUrl = "http://192.168.2.111/data"

    private val segregationTips = listOf(
        "Rinse plastic containers before throwing them in the Recyclable bin.",
        "Food-stained paper (like pizza boxes) belongs in Residual waste.",
        "Crush plastic bottles and tin cans to save space in your bins.",
        "Biodegradable waste can be used for composting your garden!",
        "Keep recyclables dry. Wet paper can ruin a whole batch of recycling.",
        "Check for the recycling symbol on plastics to sort them correctly.",
        "Batteries and electronics are hazardous; don't put them in regular bins!"
    )

    private val client = OkHttpClient()
    private val updateInterval = 4000L // 4 Seconds

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Views
        bin1Bar = view.findViewById(R.id.bin1Bar)
        bin2Bar = view.findViewById(R.id.bin2Bar)
        bin3Bar = view.findViewById(R.id.bin3Bar)
        warningText = view.findViewById(R.id.warningText)
        logoutButton = view.findViewById(R.id.buttonLogout)
        tipText = view.findViewById(R.id.textSegregationTip) // Fixed initialization

        logoutButton.setOnClickListener {
            (activity as? MainActivity)?.logoutUser()
        }

        handler = Handler(Looper.getMainLooper())

        // Start the Live Network Loop
        startAutoUpdate()
    }

    private fun startAutoUpdate() {
        handler.removeCallbacksAndMessages(null)
        val runnable = object : Runnable {
            override fun run() {
                if (isAdded && view != null) {
                    // 1. Fetch real data from ESP32
                    fetchData()

                    // 2. Rotate Tips
                    showRandomTip()

                    handler.postDelayed(this, updateInterval)
                }
            }
        }
        handler.post(runnable)
    }

    private fun fetchData() {
        // Using lifecycleScope is safer for Fragments
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(espUrl).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val json = JSONObject(responseBody)
                    // Match these keys exactly to what your ESP32 sends
                    val b1 = json.getInt("bin1")
                    val b2 = json.getInt("bin2")
                    val b3 = json.getInt("bin3")

                    withContext(Dispatchers.Main) {
                        updateUI(b1, b2, b3)
                        updateConnectionStatus(true)
                    }
                } else {
                    withContext(Dispatchers.Main) { updateConnectionStatus(false) }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error fetching data", e)
                withContext(Dispatchers.Main) { updateConnectionStatus(false) }
            }
        }
    }

    private fun updateUI(bin1: Int, bin2: Int, bin3: Int) {
        if (!isAdded || view == null) return

        bin1Bar.progress = bin1
        bin2Bar.progress = bin2
        bin3Bar.progress = bin3

        val warnings = StringBuilder()
        // Bin 1: Residual, Bin 2: Non-Residual, Bin 3: Recyclable
        if (bin1 >= 80) warnings.append("⚠️ Residual Bin is getting full\n")
        if (bin2 >= 80) warnings.append("⚠️ Non-Residual Bin is getting full\n")
        if (bin3 >= 80) warnings.append("⚠️ Recyclable Bin is getting full\n")

        if (warnings.isNotEmpty()) {
            warningText.visibility = View.VISIBLE
            warningText.text = warnings.toString().trim()
            warningText.setBackgroundColor(Color.parseColor("#FFF59D"))
        } else {
            warningText.visibility = View.GONE
        }
    }

    private fun updateConnectionStatus(online: Boolean) {
        val statusDot = view?.findViewById<View>(R.id.statusDot)
        val statusText = view?.findViewById<TextView>(R.id.statusText)

        if (online) {
            statusDot?.setBackgroundColor(Color.GREEN)
            statusText?.text = "Online"
            statusText?.setTextColor(Color.GREEN)
        } else {
            statusDot?.setBackgroundResource(R.drawable.redstatus_dot)
            statusText?.text = "Offline"
            statusText?.setTextColor(Color.RED)
        }
    }

    private fun showRandomTip() {
        if (::tipText.isInitialized) {
            tipText.text = segregationTips.random()
        }
    }

    // Keeping your simulation logic here just in case you need to test offline again
    /*
    private fun simulateLiveData() {
        val r1 = (10..95).random()
        val r2 = (10..95).random()
        val r3 = (10..95).random()
        updateUI(r1, r2, r3)
    }
    */
}