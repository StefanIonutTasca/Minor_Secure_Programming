package com.example.minor_secure_programming

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Intent


class ExportDataActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_data)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Export My Data"

        findViewById<Button>(R.id.btnExportNow).setOnClickListener {
            Toast.makeText(this, "Exporting data...", Toast.LENGTH_SHORT).show()
            // Export logic here
        }

        findViewById<BottomNavigationView>(R.id.bottomNavigation).setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java)); true
                }
                R.id.navigation_lol -> {
                    startActivity(Intent(this, LolStatsActivity::class.java)); true
                }
                R.id.navigation_dashboard -> {
                    startActivity(Intent(this, MainActivity::class.java)); true
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
