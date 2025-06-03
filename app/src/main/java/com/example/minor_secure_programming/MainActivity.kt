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
                    // Already on home page (dashboard)
                    true
                }
                R.id.navigation_lol -> {
                    // Navigate to LOL Stats page
                    val intent = Intent(this, LolStatsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_dashboard -> {
                    // Already on home page (dashboard)
                    true
                }
                else -> false
            }
        }
        
        // Set active navigation item
        bottomNav.selectedItemId = R.id.navigation_home
    }
    
    private fun setupCardClicks() {
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
        
        // Wellness card - just a toast message for now
        findViewById<CardView>(R.id.cardWellness).setOnClickListener {
            Toast.makeText(this, "Wellness features coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Settings card
        findViewById<CardView>(R.id.cardSettings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

}
