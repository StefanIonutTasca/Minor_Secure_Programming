package com.example.minor_secure_programming

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    private val SPLASH_TIME_OUT: Long = 2000 // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Hide action bar if it exists
        supportActionBar?.hide()
        
        // Create a delay and then start the main activity
        Handler(Looper.getMainLooper()).postDelayed({
            // Start main activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            
            // Close this activity
            finish()
        }, SPLASH_TIME_OUT)
    }
}
