package com.example.minor_secure_programming

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.lang.reflect.Method

/**
 * Unit tests for OverwatchStatsActivity
 * Focuses on security-critical functions like input validation
 */
class OverwatchStatsActivityTest {

    private lateinit var activity: OverwatchStatsActivity
    private lateinit var mockContext: Context
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    
    // Method for battletag validation access via reflection
    private lateinit var validateBattletagMethod: Method
    
    @Before
    fun setup() {
        // Mock context and shared preferences
        mockContext = mock()
        mockSharedPreferences = mock()
        mockEditor = mock()
        
        // Setup shared preferences mock
        whenever(mockSharedPreferences.edit()).thenReturn(mockEditor)
        whenever(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        whenever(mockEditor.apply()).then { }
        
        // Create activity instance
        activity = OverwatchStatsActivity()
        
        // Use reflection to set the mockSharedPreferences
        val sharedPrefsField = OverwatchStatsActivity::class.java.getDeclaredField("sharedPreferences")
        sharedPrefsField.isAccessible = true
        sharedPrefsField.set(activity, mockSharedPreferences)
        
        // Get access to validateBattletag method
        validateBattletagMethod = OverwatchStatsActivity::class.java.getDeclaredMethod("validateBattletag", String::class.java)
        validateBattletagMethod.isAccessible = true
    }
    
    /**
     * Test battletag validation with valid input
     */
    @Test
    fun validateBattletag_withValidInput_returnsFormattedBattletag() {
        // Test with valid battletag format
        val validBattletag = "Player-1234"
        val result = validateBattletagMethod.invoke(activity, validBattletag) as Pair<*, *>
        
        assertTrue("Valid battletag should be accepted", result.first as Boolean)
        assertEquals("Player-1234", result.second)
    }
    
    /**
     * Test battletag validation with valid battletag having lowercase characters
     */
    @Test
    fun validateBattletag_withMixedCaseBattletag_returnsFormattedVersion() {
        // Test with lowercase characters (should auto-format)
        val mixedCaseBattletag = "player-1234"
        val result = validateBattletagMethod.invoke(activity, mixedCaseBattletag) as Pair<*, *>
        
        assertTrue("Mixed case battletag should be accepted", result.first as Boolean)
        assertEquals("Player-1234", result.second)
    }
    
    /**
     * Test battletag validation with battletag containing hash
     */
    @Test
    fun validateBattletag_withHashInsteadOfDash_convertsToCorrectFormat() {
        // Test with hash instead of dash (common user input format)
        val hashBattletag = "Player#1234"
        val result = validateBattletagMethod.invoke(activity, hashBattletag) as Pair<*, *>
        
        assertTrue("Battletag with hash should be converted to dash format", result.first as Boolean)
        assertEquals("Player-1234", result.second)
    }
    
    /**
     * Test battletag validation with empty input
     */
    @Test
    fun validateBattletag_withEmptyInput_returnsError() {
        val emptyBattletag = ""
        val result = validateBattletagMethod.invoke(activity, emptyBattletag) as Pair<*, *>
        
        assertFalse("Empty battletag should be rejected", result.first as Boolean)
        assertTrue("Error message should mention requirement", 
                  (result.second as String).contains("required"))
    }
    
    /**
     * Test battletag validation with dangerous input (XSS attempt)
     */
    @Test
    fun validateBattletag_withDangerousInput_returnsError() {
        val maliciousBattletag = "Player<script>-1234"
        val result = validateBattletagMethod.invoke(activity, maliciousBattletag) as Pair<*, *>
        
        assertFalse("Malicious battletag should be rejected", result.first as Boolean)
        assertTrue("Error message should mention invalid characters", 
                  (result.second as String).contains("invalid character"))
    }
    
    /**
     * Test battletag validation with SQL injection attempt
     */
    @Test
    fun validateBattletag_withSqlInjectionAttempt_returnsError() {
        val sqlInjectionBattletag = "Player-1234' OR '1'='1"
        val result = validateBattletagMethod.invoke(activity, sqlInjectionBattletag) as Pair<*, *>
        
        assertFalse("SQL injection attempt should be rejected", result.first as Boolean)
        assertTrue("Error message should mention invalid characters",
                  (result.second as String).contains("invalid character"))
    }
    
    /**
     * Test battletag validation with very long input (potential buffer overflow)
     */
    @Test
    fun validateBattletag_withExcessiveLength_returnsError() {
        val veryLongBattletag = "Player-" + "1".repeat(1000)
        val result = validateBattletagMethod.invoke(activity, veryLongBattletag) as Pair<*, *>
        
        assertFalse("Excessively long battletag should be rejected", result.first as Boolean)
        assertTrue("Error message should mention length",
                  (result.second as String).contains("too long"))
    }
    
    /**
     * Test battletag validation with common evasion techniques
     */
    @Test
    fun validateBattletag_withUnicodeEvasionAttempt_returnsError() {
        // Unicode evasion technique
        val unicodeEvasionBattletag = "Player-12\u003Cscript\u003E34"
        val result = validateBattletagMethod.invoke(activity, unicodeEvasionBattletag) as Pair<*, *>
        
        assertFalse("Unicode evasion attempt should be rejected", result.first as Boolean)
        assertTrue("Error message should mention invalid characters",
                  (result.second as String).contains("invalid character"))
    }
}
