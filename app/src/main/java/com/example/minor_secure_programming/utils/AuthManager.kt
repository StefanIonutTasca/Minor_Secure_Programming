package com.example.minor_secure_programming.utils

import io.github.jan.supabase.auth.auth

/**
 * AuthManager - A utility class to manage authentication tokens securely
 * This class provides methods to retrieve the current authentication token
 * and helpers for Supabase authentication integration
 */
object AuthManager {
    /**
     * Gets the current authentication token from Supabase client
     * Returns null if user is not authenticated
     * 
     * @return The auth token or null
     */
    fun getCurrentToken(): String? {
        // Get session token from Supabase
        val session = SupabaseManager.client.   auth.currentSessionOrNull()
        
        // Return access token if available, null otherwise
        return session?.accessToken
    }
    
    /**
     * Check if a user is currently authenticated
     * 
     * @return true if authenticated, false otherwise
     */
    fun isAuthenticated(): Boolean {
        return SupabaseManager.client.auth.currentUserOrNull() != null
    }
}
