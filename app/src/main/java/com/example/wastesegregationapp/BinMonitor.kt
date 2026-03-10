package com.example.wastesegregationapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

class BinMonitoringService : Service() {

    private val dbUrl = "https://wise-wastee-default-rtdb.asia-southeast1.firebasedatabase.app"

    private var confirmationCountRes = 0
    private var confirmationCountNonRes = 0
    private var confirmationCountRecyc = 0

    private val REQUIRED_CONFIRMATIONS = 1

    private var lastLevelRes = 0
    private var lastLevelNonRes = 0
    private var lastLevelRecyc = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val dbRef = FirebaseDatabase.getInstance(dbUrl).getReference("bins")

        //RESIDUAL
        dbRef.child("residual/level").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val level = snapshot.getValue(Int::class.java) ?: 0

                if (level > 0 && (lastLevelRes == 0 || Math.abs(level - lastLevelRes) <= 2)) {
                    confirmationCountRes++

                    if (confirmationCountRes >= REQUIRED_CONFIRMATIONS) {
                        logDailyData(level, "residual")

                        if (level >= 90 && lastLevelRes < 90) {
                            sendNotification("Residual Bin Full!", level, 101)
                            saveNotificationToFirebase("Residual", level)
                        }

                        confirmationCountRes = REQUIRED_CONFIRMATIONS
                    }
                } else {
                    confirmationCountRes = 0
                }

                lastLevelRes = level
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        //  NON-RESIDUAL
        dbRef.child("non_residual/level").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val level = snapshot.getValue(Int::class.java) ?: 0

                if (level > 0 && (lastLevelNonRes == 0 || Math.abs(level - lastLevelNonRes) <= 2)) {
                    confirmationCountNonRes++

                    if (confirmationCountNonRes >= REQUIRED_CONFIRMATIONS) {
                        logDailyData(level, "non_residual")

                        if (level >= 90 && lastLevelNonRes < 90) {
                            sendNotification("Non-Residual Bin Full!", level, 102)
                            saveNotificationToFirebase("Non-Residual", level)
                        }

                        confirmationCountNonRes = REQUIRED_CONFIRMATIONS
                    }
                } else {
                    confirmationCountNonRes = 0
                }

                lastLevelNonRes = level
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        //RECYCLABLE
        dbRef.child("recyclable/level").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val level = snapshot.getValue(Int::class.java) ?: 0

                if (level > 0 && (lastLevelRecyc == 0 || Math.abs(level - lastLevelRecyc) <= 2)) {
                    confirmationCountRecyc++

                    if (confirmationCountRecyc >= REQUIRED_CONFIRMATIONS) {
                        logDailyData(level, "recyclable")

                        if (level >= 90 && lastLevelRecyc < 90) {
                            sendNotification("Recyclable Bin Full!", level, 103)
                            saveNotificationToFirebase("Recyclable", level)
                        }

                        confirmationCountRecyc = REQUIRED_CONFIRMATIONS
                    }
                } else {
                    confirmationCountRecyc = 0
                }

                lastLevelRecyc = level
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase Check", "Recyclable Error: ${error.message}")
            }
        })

        return START_STICKY
    }

    private fun saveNotificationToFirebase(binType: String, level: Int) {
        val notifRef = FirebaseDatabase.getInstance(dbUrl).getReference("notifications").push()
        val timestamp = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date())
        val notificationData = mapOf(
            "title" to "$binType Full",
            "message" to "Bin level reached $level%. Please empty it.",
            "timestamp" to timestamp
        )
        notifRef.setValue(notificationData)
    }

    private fun logDailyData(level: Int, binType: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = sdf.format(Date())
        val reportRef = FirebaseDatabase.getInstance(dbUrl).getReference("reports").child(currentDate).child("${binType}_max")

        reportRef.get().addOnSuccessListener { snapshot ->
            val currentMax = snapshot.getValue(Int::class.java) ?: 0
            if (level > currentMax) {
                reportRef.setValue(level)
            }
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun sendNotification(title: String, level: Int, notificationId: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(this, notificationId, intent, android.app.PendingIntent.FLAG_IMMUTABLE)

        val builder = androidx.core.app.NotificationCompat.Builder(this, "BIN_FULL_NOTIF")
            .setSmallIcon(R.drawable.alert)
            .setContentTitle(title)
            .setContentText("The bin is at $level%. Please empty it.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = androidx.core.app.NotificationManagerCompat.from(this)
        notificationManager.notify(notificationId, builder.build())
    }

    override fun onBind(intent: Intent?): IBinder? = null
}