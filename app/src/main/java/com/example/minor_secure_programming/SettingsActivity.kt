package com.example.minor_secure_programming

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.minor_secure_programming.utils.SupabaseManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.cardview.widget.CardView
import kotlinx.coroutines.launch


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
        
        // Get saved preferences
        val sharedPrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("dark_mode", false)
        
        // Set initial switch states based on saved preferences
        darkModeSwitch.isChecked = isDarkMode
        
        // Set up listeners
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this, "Notifications ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }
        
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Save dark mode preference
            sharedPrefs.edit().putBoolean("dark_mode", isChecked).apply()
            
            // Apply dark mode immediately
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            
            // Show feedback to user
            Toast.makeText(this, "Dark mode ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
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
            // Show loading feedback
            val logoutButton = findViewById<Button>(R.id.btnLogout)
            logoutButton.isEnabled = false
            logoutButton.text = "Logging out..."
            
            // Launch coroutine to call sign out function
            lifecycleScope.launch {
                val result = SupabaseManager.signOut()
                
                // Handle result on main thread
                runOnUiThread {
                    if (result.isSuccess) {
                        Toast.makeText(this@SettingsActivity, "Successfully logged out", Toast.LENGTH_SHORT).show()
                        
                        // Clear any saved user data
                        getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .clear()
                            .apply()
                        
                        // Navigate back to login screen
                        val intent = Intent(this@SettingsActivity, SignupLoginActivity::class.java)
                        // Clear back stack so user can't navigate back after logout
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        // Show error and re-enable the button
                        Toast.makeText(this@SettingsActivity, "Error logging out: ${result.exceptionOrNull()?.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                        logoutButton.isEnabled = true
                        logoutButton.text = "Log out"
                    }
                }
            }
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
                R.id.navigation_cv -> {
                    val intent = Intent(this, CVActivity::class.java)
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
