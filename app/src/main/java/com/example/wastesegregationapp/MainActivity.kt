package com.example.wastesegregationapp

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.random.Random
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 🔹 Link ProgressBars
        val bin1Progress = findViewById<ProgressBar>(R.id.bin1Progress)
        val bin2Progress = findViewById<ProgressBar>(R.id.bin2Progress)
        val bin3Progress = findViewById<ProgressBar>(R.id.bin3Progress)
        val bin4Progress = findViewById<ProgressBar>(R.id.bin4Progress)

        // 🔹 Link Alert Icons
        val bin1Alert = findViewById<ImageView>(R.id.bin1Alert)
        val bin2Alert = findViewById<ImageView>(R.id.bin2Alert)
        val bin3Alert = findViewById<ImageView>(R.id.bin3Alert)
        val bin4Alert = findViewById<ImageView>(R.id.bin4Alert)

        // 🔹 Link Total Bins Nearby
        val textTotalBins = findViewById<TextView>(R.id.textTotalBins)
        textTotalBins.text = "4" // can be dynamic later

        // 🔹 Function to update a bin’s progress + alert
        fun updateBin(progressBar: ProgressBar, alert: ImageView, value: Int) {
            progressBar.progress = value
            alert.visibility = if (value >= 80) ImageView.VISIBLE else ImageView.GONE
        }

        // 🔹 Randomize Button for Testing
        val randomizeButton = findViewById<Button>(R.id.randomizeButton)
        randomizeButton.setOnClickListener {
            val randomValues = List(4) { Random.nextInt(0, 101) }

            updateBin(bin1Progress, bin1Alert, randomValues[0])
            updateBin(bin2Progress, bin2Alert, randomValues[1])
            updateBin(bin3Progress, bin3Alert, randomValues[2])
            updateBin(bin4Progress, bin4Alert, randomValues[3])
        }

        // 🔹 Initialize once on app launch
        randomizeButton.performClick()

        // 🔹 Handle Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_bins -> {
                    Toast.makeText(this, "Bins clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_reports -> {
                    Toast.makeText(this, "Reports clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }
}