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
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.ImageButton
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var bin1Bar: ProgressBar
    private lateinit var bin2Bar: ProgressBar
    private lateinit var bin3Bar: ProgressBar
    private lateinit var warningIconRes: ImageView
    private lateinit var warningIconNonRes: ImageView
    private lateinit var warningIconRecyc: ImageView
    private lateinit var tipText: TextView
    private lateinit var handler: Handler
    private lateinit var database: DatabaseReference

    private val segregationTips = listOf(
        "Rinse plastic containers before throwing them in the Recyclable bin.",
        "Food-stained paper (like pizza boxes) belongs in Residual waste.",
        "Crush plastic bottles and tin cans to save space in your bins.",
        "Biodegradable waste can be used for composting your garden!",
        "Keep recyclables dry. Wet paper can ruin a whole batch of recycling.",
        "Check for the recycling symbol on plastics to sort them correctly.",
        "Batteries and electronics are hazardous; don't put them in regular bins!"
    )

    private val tipUpdateInterval = 10000L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bin1Bar = view.findViewById(R.id.bin1Bar)
        bin2Bar = view.findViewById(R.id.bin2Bar)
        bin3Bar = view.findViewById(R.id.bin3Bar)
        warningIconRes = view.findViewById(R.id.warningIconRes)
        warningIconNonRes = view.findViewById(R.id.warningIconNonRes)
        warningIconRecyc = view.findViewById(R.id.warningIconRecyc)
        val logoutButton = view.findViewById<ImageButton>(R.id.buttonLogout)
        tipText = view.findViewById(R.id.textSegregationTip)

        logoutButton.setOnClickListener {
            (activity as? MainActivity)?.logoutUser()
        }

        val dbUrl = "https://wise-wastee-default-rtdb.asia-southeast1.firebasedatabase.app"
        database = FirebaseDatabase.getInstance(dbUrl).getReference("bins")

        startFirebaseListener()

        handler = Handler(Looper.getMainLooper())
        startTipRotation()
    }

    private fun startFirebaseListener() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val resLevel = snapshot.child("residual/level").getValue(Int::class.java) ?: 0
                val nonResLevel = snapshot.child("non_residual/level").getValue(Int::class.java) ?: 0
                val recycLevel = snapshot.child("recyclable/level").getValue(Int::class.java) ?: 0

                if (isAdded && view != null) {
                    updateUI(resLevel, nonResLevel, recycLevel)

                    // Only log data when the day actually changes
                    checkAndSyncDailyReport(resLevel, nonResLevel, recycLevel)

                    updateConnectionStatus(true)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded) updateConnectionStatus(false)
            }
        })
    }

    private fun checkAndSyncDailyReport(res: Int, nonRes: Int, recyc: Int) {
        val prefs = requireActivity().getSharedPreferences("WastePrefs", android.content.Context.MODE_PRIVATE)
        val lastLoggedDate = prefs.getString("last_logged_date", "") ?: ""
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (lastLoggedDate.isNotEmpty() && lastLoggedDate != currentDate) {
            if (res > 10) saveToReports("Residual", res)
            if (nonRes > 10) saveToReports("Non-Residual", nonRes)
            if (recyc > 10) saveToReports("Recyclable", recyc)

            Log.d("DailyLog", "Successfully synced yesterday's data: $lastLoggedDate")
        }
        prefs.edit().putString("last_logged_date", currentDate).apply()
    }

    private fun saveToReports(binName: String, level: Int) {
        val reportsRef = FirebaseDatabase.getInstance().getReference("reports").push()
        val reportData = mapOf(
            "binType" to binName,
            "fillLevel" to level,
            "timestamp" to ServerValue.TIMESTAMP,
            "logType" to "Daily Summary"
        )
        reportsRef.setValue(reportData)
    }

    private fun updateUI(bin1: Int, bin2: Int, bin3: Int) {
        if (!isAdded || view == null) return
        setupAnimateAndColor(bin1Bar, bin1)
        setupAnimateAndColor(bin2Bar, bin2)
        setupAnimateAndColor(bin3Bar, bin3)

        // Icons show alert at 70% but don't trigger a database report record
        warningIconRes.visibility = if (bin1 >= 70) View.VISIBLE else View.GONE
        warningIconNonRes.visibility = if (bin2 >= 70) View.VISIBLE else View.GONE
        warningIconRecyc.visibility = if (bin3 >= 70) View.VISIBLE else View.GONE
    }

    private fun updateProgressBarColor(progressBar: ProgressBar, progress: Int) {
        val color = when {
            progress >= 90 -> Color.RED
            progress >= 50 -> Color.parseColor("#FFB300")
            else -> Color.parseColor("#4CAF50")
        }
        progressBar.progressTintList = ColorStateList.valueOf(color)
    }

    private fun setupAnimateAndColor(progressBar: ProgressBar, targetProgress: Int) {
        val animator = ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, targetProgress)
        animator.duration = 500
        animator.addUpdateListener { animation ->
            val currentProgress = animation.animatedValue as Int
            updateProgressBarColor(progressBar, currentProgress)
        }
        animator.start()
    }

    private fun updateConnectionStatus(online: Boolean) {
        val statusDot = view?.findViewById<View>(R.id.statusDot)
        val statusText = view?.findViewById<TextView>(R.id.statusText)

        if (online) {
            statusDot?.setBackgroundColor(Color.GREEN)
            statusText?.text = "Online (Live)"
            statusText?.setTextColor(Color.BLACK)
        } else {
            statusDot?.setBackgroundColor(Color.RED)
            statusText?.text = "Offline"
            statusText?.setTextColor(Color.RED)
        }
    }

    private fun startTipRotation() {
        val runnable = object : Runnable {
            override fun run() {
                if (isAdded && view != null) {
                    tipText.text = segregationTips.random()
                    handler.postDelayed(this, tipUpdateInterval)
                }
            }
        }
        handler.post(runnable)
    }
}