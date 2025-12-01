package com.example.wastesegregationapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast // Import for showing Toast messages
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth // Already imported, critical for logout

class MainActivity : AppCompatActivity() {

    // SharedPreferences Keys
    private val PREFS_FILE = "WiseWastePrefs"
    private val IS_LOGGED_IN = "isLoggedIn"

    // Reference to BottomNavigationView
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            // Determine which fragment to load first
            if (isUserLoggedIn()) {
                // Load the HomeFragment and enable the bottom navigation
                loadHomeDashboard()
            } else {
                // Load the LoginFragment
                replaceFragment(LoginFragment())
                // Hide bottom navigation until logged in
                bottomNav.visibility = View.GONE
            }
        }
    }

    // --- SESSION MANAGEMENT FUNCTIONS (Called by LoginFragment) ---

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

    // 🔑 LOGOUT FUNCTION (Called by HomeFragment) 🔑
    fun logoutUser() {
        Log.d("Logout", "User is logging out.")

        // 1. Sign out of Firebase Authentication
        // This is necessary to clear the Firebase-side session
        FirebaseAuth.getInstance().signOut()

        // 2. Clear SharedPreferences state (Session Management)
        // This ensures the app goes to the Login screen on restart
        saveLoginState(false)

        // 3. Navigate back to the Login Fragment
        val loginFragment = LoginFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, loginFragment)
            .commit()

        // 4. Hide the Bottom Navigation Bar
        bottomNav.visibility = View.GONE

        // Notify the user
        Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()
    }

    // --- NAVIGATION LOGIC ---

    private fun loadHomeDashboard() {
        bottomNav.visibility = View.VISIBLE

        // Load the initial fragment (Dashboard)
        replaceFragment(HomeFragment())

        // Set the listener for the bottom navigation
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
            // Use custom animations for smoother transition away from login
            setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            replace(R.id.fragment_container, fragment)
            commit()
        }
    }
}