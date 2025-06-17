package com.example.minor_secure_programming

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.minor_secure_programming.utils.SupabaseManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class ChangeUsernameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_username)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Change Username"

        val usernameInput = findViewById<EditText>(R.id.inputUsername)
        val confirmInput = findViewById<EditText>(R.id.inputConfirmUsername)
        val saveButton = findViewById<Button>(R.id.btnSaveUsername)

        saveButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val confirm = confirmInput.text.toString().trim()

            when {
                username.isBlank() || confirm.isBlank() -> {
                    Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
                }
                username != confirm -> {
                    Toast.makeText(this, "Usernames do not match", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Show progress and disable button
                    val progressBar = findViewById<ProgressBar>(R.id.progressBarUsername)
                    progressBar.visibility = View.VISIBLE
                    saveButton.isEnabled = false
                    
                    // Update username in Supabase
                    lifecycleScope.launch {
                        val success = SupabaseManager.updateUsername(username)
                        
                        runOnUiThread {
                            progressBar.visibility = View.GONE
                            saveButton.isEnabled = true
                            
                            if (success) {
                                Toast.makeText(this@ChangeUsernameActivity, "Username updated successfully!", Toast.LENGTH_SHORT).show()
                                setResult(RESULT_OK)
                                finish()
                            } else {
                                Toast.makeText(this@ChangeUsernameActivity, "Failed to update username. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.navigation_wellness -> {
                    startActivity(Intent(this, WellnessActivity::class.java))
                    true
                }
                R.id.navigation_cv -> {
                    startActivity(Intent(this, CVActivity::class.java))
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
