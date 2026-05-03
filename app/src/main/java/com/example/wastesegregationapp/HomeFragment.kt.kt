package com.example.wastesegregationapp

import android.view.animation.AlphaAnimation
import android.view.animation.Animation
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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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

    // NEW UI ELEMENTS
    private lateinit var textTotalBins: TextView
    private lateinit var imgLivePulse: ImageView
    private lateinit var statusDotBin1: View
    private lateinit var statusDotBin2: View
    private lateinit var statusDotBin3: View

    private val pollingInterval = 800L

    private var bioTimer: Handler? = null
    private var nonBioTimer: Handler? = null
    private var otherTimer: Handler? = null

    private var isBioNotified = false
    private var isNonBioNotified = false
    private var isOtherNotified = false

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

        // Existing View Initializations
        bin1Bar = view.findViewById(R.id.bin1Bar)
        bin2Bar = view.findViewById(R.id.bin2Bar)
        bin3Bar = view.findViewById(R.id.bin3Bar)

        tvPercentageBio = view.findViewById(R.id.tvPercentageRes)
        tvPercentageNonBio = view.findViewById(R.id.tvPercentageNonRes)
        tvPercentageOther = view.findViewById(R.id.tvPercentageRecyc)

        tipText = view.findViewById(R.id.textSegregationTip)
        statusText = view.findViewById(R.id.statusText)
        statusDot = view.findViewById(R.id.statusDot)

        // NEW UI View Initializations
        textTotalBins = view.findViewById(R.id.textTotalBins)
        imgLivePulse = view.findViewById(R.id.imgLivePulse)
        statusDotBin1 = view.findViewById(R.id.statusDotBin1)
        statusDotBin2 = view.findViewById(R.id.statusDotBin2)
        statusDotBin3 = view.findViewById(R.id.statusDotBin3)

        view.findViewById<ImageButton>(R.id.buttonLogout).setOnClickListener {
            (activity as? MainActivity)?.logoutUser()
        }

        handler = Handler(Looper.getMainLooper())

        startTipRotation()
        startDataPolling()
        startPulseAnimation() // Start the visual heartbeat
    }

    private fun startPulseAnimation() {
        val pulseAnimation = AlphaAnimation(1.0f, 0.2f).apply {
            duration = 1000
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
        }
        imgLivePulse.startAnimation(pulseAnimation)
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

        val request = object : StringRequest(Request.Method.GET, config.GET_DATA_URL,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.has("levels")) {
                        val levels = json.getJSONObject("levels")
                        val bio = levels.optInt("Bio", 0).coerceIn(0, 100)
                        val non = levels.optInt("Non", 0).coerceIn(0, 100)
                        val mix = levels.optInt("others", 0).coerceIn(0, 100)

                        updateUI(bio, non, mix)
                        updateConnectionStatus(true)
                    }
                } catch (e: Exception) {
                    updateConnectionStatus(false)
                }
            },
            { updateConnectionStatus(false) }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Cache-Control"] = "no-cache"
                return headers
            }
        }
        request.setShouldCache(false)
        queue.add(request)
    }

    private fun updateUI(bio: Int, nonBio: Int, other: Int) {
        setupAnimateAndColor(bin1Bar, bio)
        setupAnimateAndColor(bin2Bar, nonBio)
        setupAnimateAndColor(bin3Bar, other)

        tvPercentageBio.text = "$bio%"
        tvPercentageNonBio.text = "$nonBio%"
        tvPercentageOther.text = "$other%"

        // Sensor Status Logic: If reading is 0, we can assume offline or empty.
        // For Ultrasonic, usually "Offline" would mean the JSON didn't return a value.
        // For now, we update individual dots based on successful data fetch.
        updateIndividualBinDots(true)

        // Confirmation logic (Notifications)
        if (bio >= 90) startConfirmationTimer("Bio", { isBioNotified }, { isBioNotified = true }, bioTimer, { bioTimer = it }, warningIconBio)
        else resetConfirmation("Bio")

        if (nonBio >= 90) startConfirmationTimer("Non-Bio", { isNonBioNotified }, { isNonBioNotified = true }, nonBioTimer, { nonBioTimer = it }, warningIconNonBio)
        else resetConfirmation("Non-Bio")

        if (other >= 90) startConfirmationTimer("Others", { isOtherNotified }, { isOtherNotified = true }, otherTimer, { otherTimer = it }, warningIconOther)
        else resetConfirmation("Others")
    }

    private fun updateIndividualBinDots(online: Boolean) {
        val color = if (online) R.drawable.greendot_live else R.drawable.redstatus_dot
        statusDotBin1.setBackgroundResource(color)
        statusDotBin2.setBackgroundResource(color)
        statusDotBin3.setBackgroundResource(color)

        // Update total count: If all online, show 3. If one fails, show 2, etc.
        textTotalBins.text = if (online) "3" else "0"
    }

    private fun startConfirmationTimer(type: String, notifiedCheck: () -> Boolean, setNotified: () -> Unit, currentTimer: Handler?, setTimer: (Handler?) -> Unit, icon: ImageView) {
        if (currentTimer == null && !notifiedCheck()) {
            val newHandler = Handler(Looper.getMainLooper())
            setTimer(newHandler)
            newHandler.postDelayed({
                triggerNotification(type, "The $type bin has reached capacity.")
                setNotified()
                icon.visibility = View.VISIBLE
            }, 5000)
        }
    }

    private fun resetConfirmation(type: String) {
        when (type) {
            "Bio" -> { bioTimer?.removeCallbacksAndMessages(null); bioTimer = null; isBioNotified = false; warningIconBio.visibility = View.GONE }
            "Non-Bio" -> { nonBioTimer?.removeCallbacksAndMessages(null); nonBioTimer = null; isNonBioNotified = false; warningIconNonBio.visibility = View.GONE }
            "Others" -> { otherTimer?.removeCallbacksAndMessages(null); otherTimer = null; isOtherNotified = false; warningIconOther.visibility = View.GONE }
        }
    }

    private fun triggerNotification(binName: String, message: String) {
        val fullTitle = "$binName Bin is Full!"

        val builder = NotificationCompat.Builder(requireContext(), "BIN_FULL_NOTIF")
            .setSmallIcon(R.drawable.ic_bell_notification)
            .setContentTitle(fullTitle)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(requireContext()).notify(System.currentTimeMillis().toInt(), builder.build())
            saveNotificationToLocalDb(fullTitle, message)
        } catch (e: SecurityException) {
            Log.e("NOTIF", "Permission missing")
        }
    }

    private fun saveNotificationToLocalDb(title: String, message: String) {
        val queue = Volley.newRequestQueue(requireContext())
        val url = "http://192.168.0.147/waste_api/save_notification.php"

        val request = object : StringRequest(Request.Method.POST, url,
            { response -> Log.d("DB_SAVE", "Notification logged to DB") },
            { error -> Log.e("DB_SAVE", "Save failed: ${error.message}") }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["title"] = title
                params["message"] = message
                return params
            }
        }
        queue.add(request)
    }

    private fun setupAnimateAndColor(progressBar: ProgressBar, target: Int) {
        ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, target).apply {
            duration = 300
            addUpdateListener {
                val current = it.animatedValue as Int
                val color = when {
                    current >= 90 -> Color.RED
                    current >= 65 -> Color.parseColor("#FFB300")
                    else -> Color.parseColor("#4CAF50")
                }
                progressBar.progressTintList = ColorStateList.valueOf(color)
            }
            start()
        }
    }

    private fun updateConnectionStatus(online: Boolean) {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        activity?.runOnUiThread {
            statusDot?.setBackgroundResource(if (online) R.drawable.greendot_live else R.drawable.redstatus_dot)
            statusText?.text = if (online) "Online - Last Update: $currentTime" else "Offline - Reconnecting..."

            if (!online) {
                updateIndividualBinDots(false)
            }
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
        bioTimer?.removeCallbacksAndMessages(null)
        nonBioTimer?.removeCallbacksAndMessages(null)
        otherTimer?.removeCallbacksAndMessages(null)
    }
}