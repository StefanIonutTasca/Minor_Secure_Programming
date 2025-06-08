package com.example.minor_secure_programming

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.cardview.widget.CardView


class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // Setup action bar with back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
        
        // Initialize switches
        val notificationsSwitch = findViewById<SwitchMaterial>(R.id.switchNotifications)
        val darkModeSwitch = findViewById<SwitchMaterial>(R.id.switchDarkMode)
        val dataCollectionSwitch = findViewById<SwitchMaterial>(R.id.switchDataCollection)
        
        // Set up listeners
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this, "Notifications ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }
        
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this, "Dark mode will be ${if (isChecked) "enabled" else "disabled"} on next app restart", Toast.LENGTH_SHORT).show()
        }
        
        dataCollectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this, "Data collection ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }

        
        findViewById<Button>(R.id.btnPrivacyPolicy).setOnClickListener {
            Toast.makeText(this, "Privacy policy would open here", Toast.LENGTH_SHORT).show()
        }

        // Inside onCreate()
        findViewById<CardView>(R.id.cardAccount).setOnClickListener {
            startActivity(Intent(this, AccountSettingsActivity::class.java))
        }

        // Logout button
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
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
