package com.example.secureapp

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit test for security utilities
 */
class SecurityUtilTest {
    
    @Test
    fun testPasswordStrength() {
        // A simple test to validate password strength checking
        val weakPassword = "password123"
        val strongPassword = "P@$$w0rd!2023#Complex"
        
        assertFalse("Weak password should fail validation", SecurityUtil.isStrongPassword(weakPassword))
        assertTrue("Strong password should pass validation", SecurityUtil.isStrongPassword(strongPassword))
    }
    
    @Test
    fun testInputSanitization() {
        // Test SQL injection detection
        val safeInput = "Hello, this is a normal string"
        val maliciousInput = "'; DROP TABLE users; --"
        
        assertTrue("Safe input should pass validation", SecurityUtil.isSafeInput(safeInput))
        assertFalse("SQL injection attempt should fail validation", SecurityUtil.isSafeInput(maliciousInput))
    }
}
