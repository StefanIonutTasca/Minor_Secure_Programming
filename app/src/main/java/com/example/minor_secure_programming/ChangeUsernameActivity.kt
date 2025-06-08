package com.example.minor_secure_programming

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

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
                    Toast.makeText(this, "Username saved: $username", Toast.LENGTH_SHORT).show()
                    // Add saving logic here
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
                R.id.navigation_lol -> {
                    startActivity(Intent(this, LolStatsActivity::class.java))
                    true
                }
                R.id.navigation_dashboard -> {
                    startActivity(Intent(this, MainActivity::class.java))
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
