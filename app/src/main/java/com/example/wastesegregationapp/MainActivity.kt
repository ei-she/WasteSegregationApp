package com.example.wastesegregationapp

import DailyReport
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import java.util.concurrent.TimeUnit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager


class MainActivity : AppCompatActivity() {

    private val PREFS_FILE = "WiseWastePrefs"
    private val IS_LOGGED_IN = "isLoggedIn"

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            if (isUserLoggedIn()) {
                loadHomeDashboard()
            } else {
                replaceFragment(LoginFragment())
                bottomNav.visibility = View.GONE
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
        scheduleDailyReportUpload()
    }

    fun logoutUser() {
        Log.d("Logout", "User is logging out.")

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

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.navigation_bins -> {
                    replaceFragment(BinsFragment())
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
    fun scheduleDailyReportUpload() {
        val now = Calendar.getInstance()
        val endofDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
        }
        var delay = endofDay.timeInMillis - now.timeInMillis
        if (delay < 0) {
            delay += TimeUnit.DAYS.toMillis(1)
        }
        val dailyUploadRequest = PeriodicWorkRequestBuilder<DailyReport>(
            1, TimeUnit.DAYS
        ).setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "DailyReportUpload",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyUploadRequest)
    }
}