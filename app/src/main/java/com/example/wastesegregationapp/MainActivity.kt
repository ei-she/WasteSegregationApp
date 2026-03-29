package com.example.wastesegregationapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {
    private val PREFS_FILE = "WiseWastePrefs"
    private val IS_LOGGED_IN = "isLoggedIn"

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_navigation)

        createNotificationChannel()

        checkNotificationPermission()

        if (savedInstanceState == null) {
            if (isUserLoggedIn()) {
                loadHomeDashboard()
            } else {
                replaceFragment(LoginFragment())
                bottomNav.visibility = View.GONE
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Bin Alerts"
            val descriptionText = "Notifications for full waste bins"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel("BIN_FULL_NOTIF", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: android.app.NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun isUserLoggedIn(): Boolean {
        val prefs = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return prefs.getBoolean(IS_LOGGED_IN, false)
    }

    fun saveLoginState(isLoggedIn: Boolean) {
        val prefs = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(IS_LOGGED_IN, isLoggedIn).apply()
        Log.d("LoginState", "Login state saved: $isLoggedIn")
    }

    fun navigateToHome() {
        Log.d("Navigation", "Navigating to HomeFragment")
        loadHomeDashboard()
    }

    fun logoutUser() {
        Log.d("Logout", "User is logging out.")

        stopService(Intent(this, BinMonitor::class.java))
        FirebaseAuth.getInstance().signOut()
        saveLoginState(false)

        val loginFragment = LoginFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, loginFragment)
            .commit()

        bottomNav.visibility = View.GONE
        Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()
    }

    private fun loadHomeDashboard() {
        bottomNav.visibility = View.VISIBLE
        replaceFragment(HomeFragment())

        val serviceIntent = Intent(this, BinMonitor::class.java)
        startService(serviceIntent)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.navigation_bins -> {
                    replaceFragment(NotificationFragment())
                    true
                }
                R.id.navigation_reports -> {
                    replaceFragment(ReportsFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            replace(R.id.fragment_container, fragment)
            commit()
        }
    }
}