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
            val intent = Intent(this, SignupLoginActivity::class.java)
            startActivity(intent)
            finish()
        }, 1500) // 1.5 seconds splash
    }
}
