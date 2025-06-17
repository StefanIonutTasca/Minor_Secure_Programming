package com.example.minor_secure_programming

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.minor_secure_programming.utils.SupabaseManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    // Register for activity result from AccountSettingsActivity
    private val accountSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Username was updated, reload user profile
            loadUserProfile()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Load user profile data
        loadUserProfile()
        
        // Setup card clicks
        setupCardClickListeners()
        
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
    
    /**
     * Load user profile data from Supabase
     */
    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                // Get the user profile from Supabase
                val profile = SupabaseManager.getUserProfile()
                
                profile?.let {
                    val username = it["username"] as? String
                    val usernameText = findViewById<TextView>(R.id.usernameText)
                    
                    if (username != null) {
                        // Check if username is just an email prefix (default from our trigger)
                        if (username.contains("@") || username.startsWith("user_")) {
                            // This is likely a default username, prompt to change
                            runOnUiThread {
                                usernameText.text = username
                                showUsernameSetupDialog()
                            }
                        } else {
                            // This is a proper username
                            runOnUiThread {
                                usernameText.text = username
                            }
                        }
                    } else {
                        // No username found, prompt to set one
                        runOnUiThread {
                            showUsernameSetupDialog()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Show dialog to set up username
     */
    private fun showUsernameSetupDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_username_setup, null)
        val editUsername = dialogView.findViewById<EditText>(R.id.editUsername)
        
        builder.setView(dialogView)
            .setTitle("Set Username")
            .setMessage("Please choose a username to display on your profile")
            .setPositiveButton("Save") { dialog, _ ->
                val username = editUsername.text.toString().trim()
                if (username.isNotEmpty()) {
                    updateUsername(username)
                } else {
                    Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Later") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
        
        builder.create().show()
    }
    
    /**
     * Update the user's username in Supabase
     */
    private fun updateUsername(username: String) {
        lifecycleScope.launch {
            try {
                val success = SupabaseManager.updateUsername(username)
                
                if (success) {
                    runOnUiThread {
                        findViewById<TextView>(R.id.usernameText).text = username
                        Toast.makeText(this@MainActivity, "Username updated successfully", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Failed to update username", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error updating username: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun setupCardClickListeners() {
        // User Profile card - navigate to Account Settings
        findViewById<CardView>(R.id.userProfileCard).setOnClickListener {
            // Use the activity result launcher to start AccountSettingsActivity
            accountSettingsLauncher.launch(Intent(this, AccountSettingsActivity::class.java))
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
