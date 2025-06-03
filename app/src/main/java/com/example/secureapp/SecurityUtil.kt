package com.example.secureapp

/**
 * Security utilities for the secure application
 * Implements various security functions for password validation, 
 * encryption, and other security-related operations
 */
class SecurityUtil {
    
    companion object {
        /**
         * Validates if a password meets the security requirements
         * @param password The password to validate
         * @return true if the password is strong enough, false otherwise
         */
        fun isStrongPassword(password: String): Boolean {
            val minLength = 12
            val hasUppercase = password.any { it.isUpperCase() }
            val hasLowercase = password.any { it.isLowerCase() }
            val hasDigit = password.any { it.isDigit() }
            val hasSpecialChar = password.any { !it.isLetterOrDigit() }
            
            return password.length >= minLength && 
                   hasUppercase && 
                   hasLowercase && 
                   hasDigit && 
                   hasSpecialChar
        }
        
        /**
         * Checks if input contains potential injection attacks
         * @param input The user input to validate
         * @return true if the input is safe, false if it might contain malicious content
         */
        fun isSafeInput(input: String): Boolean {
            // Simple check for SQL injection attempts - in a real app, use parameterized queries
            val sqlInjectionPatterns = listOf(
                "DROP TABLE", "DELETE FROM", "INSERT INTO", "SELECT *", 
                "1=1", "OR 1=1", "';", "--", "/*", "*/"
            )
            
            return !sqlInjectionPatterns.any { input.uppercase().contains(it.uppercase()) }
        }
    }
}
