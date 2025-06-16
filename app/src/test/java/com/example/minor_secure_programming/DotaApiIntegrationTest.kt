package com.example.minor_secure_programming

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Test the integration between the Android app and the backend Dota API endpoints.
 * Note: These tests require the backend server to be running at the specified BASE_URL.
 */
class DotaApiIntegrationTest {

    companion object {
        // Local backend URL for testing locally
        private const val LOCAL_URL = "http://localhost:8000/api/v1"
        // Hosted backend URL for production
        private const val HOSTED_URL = "https://minor-secure-programming-backend.onrender.com/api/v1"
        // Set to false to use local backend, true to use hosted backend
        private const val USE_HOSTED_URL = false
        // Base URL to be used based on the environment setting
        private val BASE_URL = if (USE_HOSTED_URL) HOSTED_URL else LOCAL_URL
        
        // Example player IDs and match IDs for testing
        private const val SAMPLE_PLAYER_ID = "76561198025007092" // arteezy player ID
        private const val SAMPLE_MATCH_ID = "7451573499" // random match ID
    }

    @Test
    fun test_getPlayerProfile() = runBlocking {
        val url = URL("$BASE_URL/dota/profile/$SAMPLE_PLAYER_ID")
        val result = makeGetRequest(url)
        
        // Verify response contains expected fields
        assertNotNull("Response should not be null", result)
        assertTrue("Response should have 'success' field set to true", result.getBoolean("success"))
        
        val data = result.getJSONObject("data")
        assertNotNull("Data field should not be null", data)
        
        // Verify player object exists
        assertTrue("Response should contain player data", data.has("player"))
        
        // Verify win_loss data exists
        assertTrue("Response should contain win_loss data", data.has("win_loss"))
        
        // Verify recent_matches array exists
        assertTrue("Response should contain recent_matches array", data.has("recent_matches"))
        
        println("✅ Player profile test passed")
    }
    
    @Test
    fun test_getHeroes() = runBlocking {
        val url = URL("$BASE_URL/dota/heroes")
        val result = makeGetRequest(url)
        
        // Verify response contains expected fields
        assertNotNull("Response should not be null", result)
        assertTrue("Response should have 'success' field set to true", result.getBoolean("success"))
        
        val data = result.getJSONArray("data")
        assertNotNull("Data array should not be null", data)
        assertTrue("Heroes list should not be empty", data.length() > 0)
        
        // Check first hero has required fields
        val firstHero = data.getJSONObject(0)
        assertTrue("Hero should have id", firstHero.has("id"))
        assertTrue("Hero should have name", firstHero.has("name"))
        
        println("✅ Heroes test passed")
    }
    
    @Test
    fun test_getMatchDetails() = runBlocking {
        val url = URL("$BASE_URL/dota/matches/$SAMPLE_MATCH_ID")
        val result = makeGetRequest(url)
        
        // Verify response contains expected fields
        assertNotNull("Response should not be null", result)
        assertTrue("Response should have 'success' field set to true", result.getBoolean("success"))
        
        val data = result.getJSONObject("data")
        assertNotNull("Data field should not be null", data)
        
        // Match should have a match_id field that matches our request
        assertEquals("Match ID should match requested ID", 
                    SAMPLE_MATCH_ID, data.getString("match_id"))
        
        println("✅ Match details test passed")
    }

    /**
     * Helper function to make a GET request and parse the JSON response
     */
    private suspend fun makeGetRequest(url: URL): JSONObject {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            
            return JSONObject(response.toString())
        } else {
            throw Exception("HTTP error: $responseCode")
        }
    }
}
