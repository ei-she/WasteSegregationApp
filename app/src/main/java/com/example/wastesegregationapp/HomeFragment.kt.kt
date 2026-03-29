package com.example.wastesegregationapp

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var bin1Bar: ProgressBar
    private lateinit var bin2Bar: ProgressBar
    private lateinit var bin3Bar: ProgressBar
    private lateinit var tvPercentageBio: TextView
    private lateinit var tvPercentageNonBio: TextView
    private lateinit var tvPercentageOther: TextView
    private lateinit var warningIconBio: ImageView
    private lateinit var warningIconNonBio: ImageView
    private lateinit var warningIconOther: ImageView
    private lateinit var tipText: TextView
    private lateinit var statusText: TextView
    private lateinit var statusDot: View
    private lateinit var handler: Handler

    private val pollingInterval = 3000L // 3 seconds

    private val segregationTips = listOf(
        "Rinse plastic containers before throwing them in the bin.",
        "Food-stained paper belongs in the Biodegradable waste.",
        "Crush plastic bottles and tin cans to save space in your bins.",
        "Keep recyclables dry. Wet paper can ruin a whole batch.",
        "Ensure the lid is closed to prevent odors from spreading."
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI Elements
        bin1Bar = view.findViewById(R.id.bin1Bar)
        bin2Bar = view.findViewById(R.id.bin2Bar)
        bin3Bar = view.findViewById(R.id.bin3Bar)

        tvPercentageBio = view.findViewById(R.id.tvPercentageRes)
        tvPercentageNonBio = view.findViewById(R.id.tvPercentageNonRes)
        tvPercentageOther = view.findViewById(R.id.tvPercentageRecyc)

        warningIconBio = view.findViewById(R.id.warningIconRes)
        warningIconNonBio = view.findViewById(R.id.warningIconNonRes)
        warningIconOther = view.findViewById(R.id.warningIconRecyc)

        tipText = view.findViewById(R.id.textSegregationTip)
        statusText = view.findViewById(R.id.statusText)
        statusDot = view.findViewById(R.id.statusDot)

        view.findViewById<ImageButton>(R.id.buttonLogout).setOnClickListener {
            (activity as? MainActivity)?.logoutUser()
        }

        handler = Handler(Looper.getMainLooper())

        startTipRotation()
        startDataPolling()
    }

    private fun startDataPolling() {
        val runnable = object : Runnable {
            override fun run() {
                if (isAdded && view != null) {
                    fetchDataFromPi()
                    handler.postDelayed(this, pollingInterval)
                }
            }
        }
        handler.post(runnable)
    }

    private fun fetchDataFromPi() {
        if (!isAdded) return
        val queue = Volley.newRequestQueue(requireContext())

        val request = StringRequest(Request.Method.GET, config.GET_DATA_URL,
            { response ->
                try {
                    Log.d("BIN_DATA", "Raw Response: $response")
                    val json = JSONObject(response)

                    // Use coerceIn(0, 100) to keep bars within valid range
                    val bio = json.optInt("bio", 0).coerceIn(0, 100)
                    val non = json.optInt("non", 0).coerceIn(0, 100)
                    val mix = json.optInt("mix", 0).coerceIn(0, 100)

                    updateUI(bio, non, mix)
                    updateConnectionStatus(true)
                } catch (e: Exception) {
                    Log.e("BIN_DATA", "Parsing Error: ${e.message}")
                    updateConnectionStatus(false)
                }
            },
            { error ->
                Log.e("BIN_DATA", "Network Error: ${error.message}")
                updateConnectionStatus(false)
            }
        )
        queue.add(request)
    }

    private fun updateUI(bio: Int, nonBio: Int, other: Int) {
        setupAnimateAndColor(bin1Bar, bio)
        setupAnimateAndColor(bin2Bar, nonBio)
        setupAnimateAndColor(bin3Bar, other)

        tvPercentageBio.text = "$bio%"
        tvPercentageNonBio.text = "$nonBio%"
        tvPercentageOther.text = "$other%"

        warningIconBio.visibility = if (bio >= 85) View.VISIBLE else View.GONE
        warningIconNonBio.visibility = if (nonBio >= 85) View.VISIBLE else View.GONE
        warningIconOther.visibility = if (other >= 85) View.VISIBLE else View.GONE
    }

    private fun setupAnimateAndColor(progressBar: ProgressBar, target: Int) {
        ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, target).apply {
            duration = 1000 // Smooth 1-second animation
            addUpdateListener {
                val current = it.animatedValue as Int
                val color = when {
                    current >= 90 -> Color.RED
                    current >= 65 -> Color.parseColor("#FFB300") // Orange
                    else -> Color.parseColor("#4CAF50") // Green
                }
                progressBar.progressTintList = ColorStateList.valueOf(color)
            }
            start()
        }
    }

    private fun updateConnectionStatus(online: Boolean) {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        activity?.runOnUiThread {
            statusDot.setBackgroundColor(if (online) Color.GREEN else Color.RED)
            statusText.text = if (online) "Online - Last Update: $currentTime" else "Offline - Reconnecting..."
        }
    }

    private fun startTipRotation() {
        val runnable = object : Runnable {
            override fun run() {
                if (isAdded && view != null) {
                    tipText.text = segregationTips.random()
                    handler.postDelayed(this, 15000L)
                }
            }
        }
        handler.post(runnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }
}