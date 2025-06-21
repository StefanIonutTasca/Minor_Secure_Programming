package com.example.minor_secure_programming.api

import android.content.Context
import com.example.minor_secure_programming.utils.AuthManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.net.HttpURLConnection

/**
 * Unit tests for ApiService focusing on security aspects including:
 * - Input sanitization
 * - Authentication token handling
 * - Network security
 * - Error handling
 * 
 * These tests use MockWebServer to simulate API responses without requiring a real backend.
 */
@ExperimentalCoroutinesApi
class ApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: ApiService
    private lateinit var mockContext: Context
    
    // Test constants
    private val testToken = "test.jwt.token"
    private val testBaseUrl = "http://localhost:8000/api/v1"
    private val testBattletag = "Player#1234"
    private val testPlayerId = "123456789"
    private val testMatchId = "987654321"
    private val mockedApiResponse = """{"success":true,"data":{"id":123,"name":"Test Player"}}"""
    private val mockedArrayResponse = """{"success":true,"data":[{"id":1,"name":"Hero1"},{"id":2,"name":"Hero2"}]}"""
    private val mockedErrorResponse = """{"success":false,"error":"Test error message"}"""
    
    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        // Mock context
        mockContext = mock()
        
        // Create a test instance of ApiService with reflection to set the base URL to our mock server
        apiService = ApiService(mockContext)
        
        // Use reflection to set the BASE_URL to our mock server URL
        val companionObject = ApiService::class.java.getDeclaredField("Companion").get(null)
        val baseUrlField = ApiService.Companion::class.java.getDeclaredField("BASE_URL")
        baseUrlField.isAccessible = true
        baseUrlField.set(companionObject, mockWebServer.url("/api/v1").toString())
    }
    
    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }
    
    /**
     * Test input sanitization function
     * This test verifies that potentially dangerous characters are removed from input
     */
    @Test
    fun sanitizeInput_removesInjectionCharacters() {
        // Using reflection to access private method
        val sanitizeMethod = ApiService::class.java.getDeclaredMethod("sanitizeInput", String::class.java)
        sanitizeMethod.isAccessible = true
        
        val unsafeInput = "Player<script>alert('XSS')</script>#1234"
        val result = sanitizeMethod.invoke(apiService, unsafeInput) as String
        
        // Verify dangerous characters are removed
        assertFalse("Script tags should be removed", result.contains("<script>"))
        assertFalse("Script tags should be removed", result.contains("</script>"))
        assertEquals("Playeralert('XSS')#1234", result)
    }
    
    /**
     * Test authentication token handling
     * Verifies that the API service correctly adds authentication headers
     */
    @Test
    fun makeGetRequest_addsAuthTokenWhenAvailable() = runTest {
        // Mock AuthManager to return our test token
        mockkObject(AuthManager)
        whenever(AuthManager.getCurrentToken()).thenReturn(testToken)
        
        // Set up mock response
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(mockedApiResponse))
            
        // Make request
        val result = apiService.getOverwatchPlayerProfile("Player#1234")
        
        // Get the recorded request
        val recordedRequest = mockWebServer.takeRequest()
        
        // Verify auth header was added
        val authHeader = recordedRequest.getHeader("Authorization")
        assertNotNull("Authorization header should be present", authHeader)
        assertEquals("Bearer $testToken", authHeader)
        
        // Verify result is successful
        assertTrue("Request should succeed", result.isSuccess)
        
        // Clean up mock
        unmockkObject(AuthManager)
    }
    
    /**
     * Test proper URL construction for Overwatch profiles
     * Validates that APIs construct valid URLs with sanitized inputs
     */
    @Test
    fun getOverwatchPlayerProfile_constructsCorrectUrlWithSanitizedInput() = runTest {
        // Input with injection characters
        val unsafeBattletag = "Player<script>#1234"
        
        // Mock a successful response
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(mockedApiResponse))
            
        // Call the method
        apiService.getOverwatchPlayerProfile(unsafeBattletag)
        
        // Get the recorded request
        val recordedRequest = mockWebServer.takeRequest()
        
        // Verify the request path doesn't contain dangerous characters
        val path = recordedRequest.path
        assertNotNull("Request path should not be null", path)
        assertFalse("Path should not contain script tags", path!!.contains("<script>"))
        assertTrue("Path should contain sanitized battletag", path.endsWith("/overwatch/profile/Player#1234"))
    }
    
    /**
     * Test error handling for network failures
     * Ensures the service properly handles and reports errors
     */
    @Test
    fun makeGetRequest_handlesErrorResponse() = runTest {
        // Mock an error response
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
            .setBody(mockedErrorResponse))
            
        // Call method
        val result = apiService.getOverwatchPlayerProfile("Player#1234")
        
        // Verify result is a failure
        assertTrue("Result should be failure", result.isFailure)
        
        // Verify error message
        val exception = result.exceptionOrNull()
        assertNotNull("Exception should not be null", exception)
        assertTrue("Exception should contain HTTP error code", 
            exception!!.message?.contains("HTTP Error 400") == true)
    }
    
    /**
     * Test Dota 2 player profile endpoint
     */
    @Test
    fun getDotaPlayerProfile_returnsPlayerData() = runTest {
        // Mock a successful response
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(mockedApiResponse))
            
        // Call method
        val result = apiService.getDotaPlayerProfile(testPlayerId)
        
        // Verify result
        assertTrue("Request should succeed", result.isSuccess)
        val json = result.getOrNull()
        assertNotNull("JSON result should not be null", json)
        assertTrue("JSON should have success field", json!!.has("success"))
        assertTrue("Success should be true", json.getBoolean("success"))
        
        // Verify correct URL was constructed
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/api/v1/dota/profile/$testPlayerId", recordedRequest.path)
    }
    
    /**
     * Test Dota 2 heroes list endpoint
     */
    @Test
    fun getDotaHeroes_returnsHeroesArray() = runTest {
        // Mock a successful response with array data
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(mockedArrayResponse))
            
        // Call method
        val result = apiService.getDotaHeroes()
        
        // Verify result
        assertTrue("Request should succeed", result.isSuccess)
        val jsonArray = result.getOrNull()
        assertNotNull("JSON array result should not be null", jsonArray)
        assertEquals("Array should have 2 items", 2, jsonArray!!.length())
        
        // Verify array contains expected data
        val firstHero = jsonArray.getJSONObject(0)
        assertEquals(1, firstHero.getInt("id"))
        assertEquals("Hero1", firstHero.getString("name"))
    }
    
    /**
     * Test Dota 2 match details endpoint
     */
    @Test
    fun getDotaMatchDetails_returnsMatchData() = runTest {
        // Mock a successful response
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(mockedApiResponse))
            
        // Call method
        val result = apiService.getDotaMatchDetails(testMatchId)
        
        // Verify result
        assertTrue("Request should succeed", result.isSuccess)
        
        // Verify correct URL was constructed
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/api/v1/dota/matches/$testMatchId", recordedRequest.path)
    }
    
    /**
     * Test Overwatch combined profile endpoint
     */
    @Test
    fun getOverwatchCombinedProfile_returnsCombinedData() = runTest {
        // Mock a successful response
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(mockedApiResponse))
            
        // Call method
        val result = apiService.getOverwatchCombinedProfile(testBattletag)
        
        // Verify result
        assertTrue("Request should succeed", result.isSuccess)
        
        // Verify URL construction and sanitization
        val recordedRequest = mockWebServer.takeRequest()
        assertTrue("Path should contain the endpoint",
            recordedRequest.path?.contains("/overwatch/players/") == true)
        assertTrue("Path should contain the battletag",
            recordedRequest.path?.contains(testBattletag) == true)
    }
    
    /**
     * Test connection timeout handling
     */
    @Test
    fun makeGetRequest_handlesTimeout() = runTest {
        // Mock a delayed response that will cause a timeout
        mockWebServer.enqueue(MockResponse()
            .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE))
            
        // Call method with a short timeout
        val result = apiService.getOverwatchPlayerProfile(testBattletag)
        
        // Verify result is a failure
        assertTrue("Result should be failure on timeout", result.isFailure)
    }
}
