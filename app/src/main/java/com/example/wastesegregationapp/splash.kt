package com.example.wastesegregationapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    // Define the duration for the splash screen
    private val SPLASH_DELAY_MS = 3000L // 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This line MUST execute quickly to display the screen.
        setContentView(R.layout.activity_splash)

        // Use a Handler to wait for 3 seconds, then launch the main activity.
        Handler(Looper.getMainLooper()).postDelayed({
            // Launch the main activity after the delay
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)

            // Finish the splash activity so the user can't return to it
            finish()
        }, SPLASH_DELAY_MS)
    }
    // All animation and TextView code is removed for maximum simplicity.
}