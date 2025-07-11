package com.example.minor_secure_programming

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class AccountSettingsActivity : AppCompatActivity() {
    
    // Register for result from ChangeUsernameActivity
    private val changeUsernameResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Set result to notify MainActivity to refresh the username
            setResult(Activity.RESULT_OK)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Account Settings"

        findViewById<Button>(R.id.btnChangeUsername).setOnClickListener {
            // Use the launcher to start the activity instead of simple startActivity
            changeUsernameResultLauncher.launch(Intent(this, ChangeUsernameActivity::class.java))
        }

        findViewById<Button>(R.id.btnExportData).setOnClickListener {
            startActivity(Intent(this, ExportDataActivity::class.java))
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
