package com.example.minor_secure_programming

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView

class GamesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_games)
        
        // Setup action bar with back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Games"
        
        // Set up click listeners for game cards
        findViewById<CardView>(R.id.cardLol).setOnClickListener {
            val intent = Intent(this, LolStatsActivity::class.java)
            startActivity(intent)
        }
        
        findViewById<CardView>(R.id.cardR6).setOnClickListener {
            Toast.makeText(this, "Rainbow Six Siege stats coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<CardView>(R.id.cardRunescape).setOnClickListener {
            Toast.makeText(this, "Old School Runescape stats coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<CardView>(R.id.cardValorant).setOnClickListener {
            Toast.makeText(this, "Valorant stats coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<CardView>(R.id.cardDota).setOnClickListener {
            Toast.makeText(this, "Dota 2 stats coming soon!", Toast.LENGTH_SHORT).show()
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
                R.id.navigation_dashboard -> {
                    val intent = Intent(this, MainActivity::class.java)
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
