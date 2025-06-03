package com.example.minor_secure_programming

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Set up button to navigate to LOL Stats page
        val btnLolStats = findViewById<Button>(R.id.btnLolStats)
        btnLolStats.setOnClickListener {
            val intent = Intent(this, LolStatsActivity::class.java)
            startActivity(intent)
        }
        
        // Initialize the bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on home page
                    true
                }
                R.id.nav_stats -> {
                    // Navigate to LOL Stats page
                    val intent = Intent(this, LolStatsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_profile -> {
                    // Would navigate to profile page
                    true
                }
                else -> false
            }
        }
        
        // Set active navigation item
        bottomNav.selectedItemId = R.id.nav_home
    }
}
