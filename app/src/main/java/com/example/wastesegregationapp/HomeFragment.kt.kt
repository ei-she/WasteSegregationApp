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

class HomeFragment : Fragment() {

    private lateinit var bin1Bar: ProgressBar
    private lateinit var bin2Bar: ProgressBar
    private lateinit var bin3Bar: ProgressBar
    private lateinit var tvPercentageRes: TextView
    private lateinit var tvPercentageNonRes: TextView
    private lateinit var tvPercentageRecyc: TextView
    private lateinit var warningIconRes: ImageView
    private lateinit var warningIconNonRes: ImageView
    private lateinit var warningIconRecyc: ImageView
    private lateinit var tipText: TextView
    private lateinit var handler: Handler

    // Polling Interval: 3 seconds for a very "live" feel during defense
    private val pollingInterval = 3000L

    private val segregationTips = listOf(
        "Rinse plastic containers before throwing them in the Recyclable bin.",
        "Food-stained paper (like pizza boxes) belongs in Residual waste.",
        "Crush plastic bottles and tin cans to save space in your bins.",
        "Keep recyclables dry. Wet paper can ruin a whole batch of recycling.",
        "Check for the recycling symbol on plastics to sort them correctly."
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Initialize UI Elements
        bin1Bar = view.findViewById(R.id.bin1Bar)
        bin2Bar = view.findViewById(R.id.bin2Bar)
        bin3Bar = view.findViewById(R.id.bin3Bar)
        tvPercentageRes = view.findViewById(R.id.tvPercentageRes)
        tvPercentageNonRes = view.findViewById(R.id.tvPercentageNonRes)
        tvPercentageRecyc = view.findViewById(R.id.tvPercentageRecyc)
        warningIconRes = view.findViewById(R.id.warningIconRes)
        warningIconNonRes = view.findViewById(R.id.warningIconNonRes)
        warningIconRecyc = view.findViewById(R.id.warningIconRecyc)
        tipText = view.findViewById(R.id.textSegregationTip)

        view.findViewById<ImageButton>(R.id.buttonLogout).setOnClickListener {
            (activity as? MainActivity)?.logoutUser()
        }

        handler = Handler(Looper.getMainLooper())

        startTipRotation()
        startDataPolling() // Replaces startFirebaseListener
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
        // Ensure you have: implementation("com.android.volley:volley:1.2.1") in build.gradle
        val queue = Volley.newRequestQueue(requireContext())

        val request = StringRequest(Request.Method.GET, config.GET_DATA_URL,
            { response ->
                try {
                    val json = JSONObject(response)

                    // These keys must match your PHP: "residual", "non_residual", "recyclable"
                    val res = json.getInt("residual")
                    val nonRes = json.getInt("non_residual")
                    val recyc = json.getInt("recyclable")

                    updateUI(res, nonRes, recyc)
                    updateConnectionStatus(true)
                } catch (e: Exception) {
                    Log.e("DATA_FETCH", "Parsing error: ${e.message}")
                }
            },
            { error ->
                Log.e("DATA_FETCH", "Server Unreachable: ${error.message}")
                updateConnectionStatus(false)
            }
        )
        queue.add(request)
    }

    private fun updateUI(bin1: Int, bin2: Int, bin3: Int) {
        setupAnimateAndColor(bin1Bar, bin1)
        setupAnimateAndColor(bin2Bar, bin2)
        setupAnimateAndColor(bin3Bar, bin3)

        tvPercentageRes.text = "$bin1%"
        tvPercentageNonRes.text = "$bin2%"
        tvPercentageRecyc.text = "$bin3%"

        // Icons appear at 70% capacity
        warningIconRes.visibility = if (bin1 >= 70) View.VISIBLE else View.GONE
        warningIconNonRes.visibility = if (bin2 >= 70) View.VISIBLE else View.GONE
        warningIconRecyc.visibility = if (bin3 >= 70) View.VISIBLE else View.GONE
    }

    private fun setupAnimateAndColor(progressBar: ProgressBar, target: Int) {
        ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, target).apply {
            duration = 500
            addUpdateListener {
                val current = it.animatedValue as Int
                val color = when {
                    current >= 90 -> Color.RED
                    current >= 50 -> Color.parseColor("#FFB300")
                    else -> Color.parseColor("#4CAF50")
                }
                progressBar.progressTintList = ColorStateList.valueOf(color)
            }
            start()
        }
    }

    private fun updateConnectionStatus(online: Boolean) {
        val statusDot = view?.findViewById<View>(R.id.statusDot)
        val statusText = view?.findViewById<TextView>(R.id.statusText)
        statusDot?.setBackgroundColor(if (online) Color.GREEN else Color.RED)
        statusText?.text = if (online) "Online (Local Pi)" else "Offline (Check Wi-Fi)"
    }

    private fun startTipRotation() {
        val runnable = object : Runnable {
            override fun run() {
                if (isAdded && view != null) {
                    tipText.text = segregationTips.random()
                    handler.postDelayed(this, 10000L)
                }
            }
        }
        handler.post(runnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null) // Stop polling when user leaves fragment
    }
}