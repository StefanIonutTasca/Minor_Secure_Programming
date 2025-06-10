package com.example.minor_secure_programming

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Setup card clicks
        setupCardClicks()
        
        // Initialize the bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Already on home page
                    true
                }
                R.id.navigation_wellness -> {
                    val intent = Intent(this, WellnessActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_cv -> {
                    val intent = Intent(this, CVActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        
        // Set the active item in the bottom navigation
        bottomNav.selectedItemId = R.id.navigation_home
    }
    
    private fun setupCardClicks() {
        // User Profile card - navigate to Account Settings
        findViewById<CardView>(R.id.userProfileCard).setOnClickListener {
            val intent = Intent(this, AccountSettingsActivity::class.java)
            startActivity(intent)
        }
        
        // Games card
        findViewById<CardView>(R.id.cardGames).setOnClickListener {
            val intent = Intent(this, GamesActivity::class.java)
            startActivity(intent)
        }
        
        // Stats card - redirect to LOL Stats for now
        findViewById<CardView>(R.id.cardStats).setOnClickListener {
            val intent = Intent(this, LolStatsActivity::class.java)
            startActivity(intent)
        }
        
        // Wellness card - navigate to Wellness activity
        findViewById<CardView>(R.id.cardWellness).setOnClickListener {
            val intent = Intent(this, WellnessActivity::class.java)
            startActivity(intent)
        }
        
        // Settings card
        findViewById<CardView>(R.id.cardSettings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

}
