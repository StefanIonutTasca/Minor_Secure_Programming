package com.example.minor_secure_programming

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.example.minor_secure_programming.utils.SupabaseManager

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Apply saved dark mode preference on app startup
        val sharedPrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("dark_mode", false)
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        
        // Initialize Supabase client
        SupabaseManager.initialize(this)
    }
}
