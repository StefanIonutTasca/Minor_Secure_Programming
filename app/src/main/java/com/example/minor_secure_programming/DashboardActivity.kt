package com.example.minor_secure_programming

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        
        // Setup action bar with back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Dashboard"
        
        // Set up click listeners for dashboard tiles
        findViewById<CardView>(R.id.cardGames).setOnClickListener {
            // In a real app, navigate to games section
            // For now, just show the LoL stats page as an example
            val intent = Intent(this, LolStatsActivity::class.java)
            startActivity(intent)
        }
        
        findViewById<CardView>(R.id.cardStats).setOnClickListener {
            val intent = Intent(this, LolStatsActivity::class.java)
            startActivity(intent)
        }
        
        findViewById<CardView>(R.id.cardWellness).setOnClickListener {
            val intent = Intent(this, WellnessActivity::class.java)
            startActivity(intent)
        }
        
        findViewById<CardView>(R.id.cardSettings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        
        // Initialize the bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_wellness -> {
                    val intent = Intent(this, WellnessActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
