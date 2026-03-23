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
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

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
    private lateinit var database: DatabaseReference

    private val dbUrl = "https://wise-wastee-default-rtdb.asia-southeast1.firebasedatabase.app"

    private val segregationTips = listOf(
        "Rinse plastic containers before throwing them in the Recyclable bin.",
        "Food-stained paper (like pizza boxes) belongs in Residual waste.",
        "Crush plastic bottles and tin cans to save space in your bins.",
        "Biodegradable waste can be used for composting your garden!",
        "Keep recyclables dry. Wet paper can ruin a whole batch of recycling.",
        "Check for the recycling symbol on plastics to sort them correctly.",
        "Batteries and electronics are hazardous; don't put them in regular bins!"
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
        database = FirebaseDatabase.getInstance(dbUrl).getReference("bins")
        handler = Handler(Looper.getMainLooper())
        startTipRotation()
        startFirebaseListener()
    }

    private fun startFirebaseListener() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || view == null) return

                val resLevel = snapshot.child("residual/level").getValue(Int::class.java) ?: 0
                val nonResLevel = snapshot.child("non_residual/level").getValue(Int::class.java) ?: 0
                val recycLevel = snapshot.child("recyclable/level").getValue(Int::class.java) ?: 0

                updateUI(resLevel, nonResLevel, recycLevel)

                // IMPORTANT: This triggers the report update immediately
                handleLiveReporting(resLevel, nonResLevel, recycLevel)

                updateConnectionStatus(true)
            }
            override fun onCancelled(error: DatabaseError) {
                if (isAdded) updateConnectionStatus(false)
            }
        })
    }

    private fun handleLiveReporting(res: Int, nonRes: Int, recyc: Int) {
        // Set to 10 for testing, change to 90 for final submission
        val threshold = 10

        if (res >= threshold) sendReportToFirebase("Residual", res)
        if (nonRes >= threshold) sendReportToFirebase("Non-Residual", nonRes)
        if (recyc >= threshold) sendReportToFirebase("Recyclable", recyc)
    }

    private fun sendReportToFirebase(binName: String, level: Int) {
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val timeKey = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val reportRef = FirebaseDatabase.getInstance(dbUrl).getReference("reports")
            .child(dateKey)
            .child("${timeKey}_$binName")

        val data = mapOf(
            "binType" to binName,
            "fillLevel" to level,
            "timestamp" to ServerValue.TIMESTAMP
        )

        reportRef.setValue(data).addOnSuccessListener {
            Log.d("LIVE_REPORT", "Sent $binName level ($level%) to graph.")
        }
    }

    private fun updateUI(bin1: Int, bin2: Int, bin3: Int) {
        if (!::warningIconRes.isInitialized) return

        setupAnimateAndColor(bin1Bar, bin1)
        setupAnimateAndColor(bin2Bar, bin2)
        setupAnimateAndColor(bin3Bar, bin3)

        tvPercentageRes.text = "$bin1%"
        tvPercentageNonRes.text = "$bin2%"
        tvPercentageRecyc.text = "$bin3%"

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
        statusText?.text = if (online) "Online (Live)" else "Offline"
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
}