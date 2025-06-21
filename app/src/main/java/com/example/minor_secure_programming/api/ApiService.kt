package com.example.minor_secure_programming.api

import android.content.Context
import android.util.Log
import com.example.minor_secure_programming.utils.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Service for making API calls to our backend
 */
class ApiService(private val context: Context) {
    
    companion object {
        // Local backend URL for emulator testing (10.0.2.2 is localhost from Android emulator)
        // NOTE: For development and testing, we can temporarily use HTTP, but should use HTTPS in production
        private const val LOCAL_URL = "http://10.0.2.2:8000/api/v1"
        // Hosted backend URL for production
        private const val HOSTED_URL = "https://minor-secure-programming-backend.onrender.com/api/v1"
        // Set to false to use local backend, true to use hosted backend
        private const val USE_HOSTED_URL = false  // Using local backend for testing
        // Base URL to be used based on the environment setting
        private val BASE_URL = if (USE_HOSTED_URL) HOSTED_URL else LOCAL_URL
        private const val TAG = "ApiService"
        // Temporary debugging flag - REMOVE IN PRODUCTION
        private const val DEBUG = true
        // Longer timeouts for development
        private const val CONNECTION_TIMEOUT = 30000  // 30 seconds
        private const val READ_TIMEOUT = 30000       // 30 seconds
    }
    
    /**
     * Get Dota 2 player profile by account ID or pro player nickname
     */
    suspend fun getDotaPlayerProfile(playerId: String): Result<JSONObject> {
        return makeGetRequest("$BASE_URL/dota/profile/$playerId")
    }
    
    /**
     * Get Dota 2 heroes list
     */
    suspend fun getDotaHeroes(): Result<JSONArray> {
        val result = makeGetRequest("$BASE_URL/dota/heroes")
        if (result.isSuccess) {
            val jsonObject = result.getOrNull()
            val data = jsonObject?.optJSONArray("data")
            if (data != null) {
                return Result.success(data)
            }
            return Result.failure(Exception("Failed to parse heroes data"))
        }
        return Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
    }
    
    /**
     * Get Dota 2 recent matches for player
     */
    suspend fun getDotaPlayerMatches(playerId: String, limit: Int = 10): Result<JSONArray> {
        val result = makeGetRequest("$BASE_URL/dota/players/$playerId/recent-matches?limit=$limit")
        if (result.isSuccess) {
            val jsonObject = result.getOrNull()
            val data = jsonObject?.optJSONArray("data")
            if (data != null) {
                return Result.success(data)
            }
            return Result.failure(Exception("Failed to parse matches data"))
        }
        return Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
    }
    
    /**
     * Get Dota 2 match details
     */
    suspend fun getDotaMatchDetails(matchId: String): Result<JSONObject> {
        val result = makeGetRequest("$BASE_URL/dota/matches/$matchId")
        if (result.isSuccess) {
            val jsonObject = result.getOrNull()
            val data = jsonObject?.optJSONObject("data")
            if (data != null) {
                return Result.success(data)
            }
            return Result.failure(Exception("Failed to parse match details"))
        }
        return Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
    }
    
    /**
     * Get Overwatch player profile by battletag
     */
    suspend fun getOverwatchPlayerProfile(battletag: String): Result<JSONObject> {
        // Sanitize battletag to prevent injection
        val sanitizedBattletag = sanitizeInput(battletag)
        
        // TEMPORARY DEBUG LOG - REMOVE IN PRODUCTION
        if (DEBUG) Log.d(TAG, "Fetching Overwatch profile for: $battletag using base URL: $BASE_URL")
        
        return makeGetRequest("$BASE_URL/overwatch/profile/$sanitizedBattletag")
    }
    
    /**
     * Get Overwatch player competitive rankings
     */
    suspend fun getOverwatchPlayerCompetitive(battletag: String): Result<JSONObject> {
        // Sanitize battletag to prevent injection
        val sanitizedBattletag = sanitizeInput(battletag)
        return makeGetRequest("$BASE_URL/overwatch/players/$sanitizedBattletag/competitive")
    }
    
    /**
     * Get Overwatch player summary statistics
     */
    suspend fun getOverwatchPlayerSummary(battletag: String): Result<JSONObject> {
        // Sanitize battletag to prevent injection
        val sanitizedBattletag = sanitizeInput(battletag)
        return makeGetRequest("$BASE_URL/overwatch/players/$sanitizedBattletag/summary")
    }
    
    /**
     * Get Overwatch heroes list
     */
    suspend fun getOverwatchHeroes(): Result<JSONObject> {
        return makeGetRequest("$BASE_URL/overwatch/heroes")
    }
    
    /**
     * Get Overwatch maps list
     */
    suspend fun getOverwatchMaps(): Result<JSONObject> {
        return makeGetRequest("$BASE_URL/overwatch/maps")
    }
    
    /**
     * Get combined Overwatch player profile
     */
    suspend fun getOverwatchCombinedProfile(battletag: String): Result<JSONObject> {
        // Sanitize battletag to prevent injection
        val sanitizedBattletag = sanitizeInput(battletag)
        return makeGetRequest("$BASE_URL/overwatch/profile/$sanitizedBattletag")
    }
    
    /**
     * Generic method to make a GET request to the API
     */
    /**
     * Make a GET request to the specified URL and return the response as a JSON object
     * @param urlString The URL to make the request to
     * @return Result containing a JSONObject with the response or an exception if the request failed
     */
    private suspend fun makeGetRequest(urlString: String): Result<JSONObject> {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            
            // TEMPORARY DEBUG LOG - REMOVE IN PRODUCTION
            if (DEBUG) Log.d(TAG, "Making GET request to: $urlString")
            
            try { // Ensure URL is properly sanitized
                val sanitizedUrl = sanitizeInput(urlString)
                val url = URL(sanitizedUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = CONNECTION_TIMEOUT
                connection.readTimeout = READ_TIMEOUT
                connection.useCaches = false
                
                // TEMPORARY DEBUG LOG - REMOVE IN PRODUCTION
                if (DEBUG) Log.d(TAG, "Connection timeout set to: $CONNECTION_TIMEOUT ms")
                
                // Add security headers
                connection.setRequestProperty("User-Agent", "GamerCV-AndroidApp")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("X-Requested-With", "XMLHttpRequest")
                
                // Get authentication token if available
                val token = AuthManager.getCurrentToken()
                if (token != null) {
                    // Add authentication token to request
                    if (DEBUG) Log.d(TAG, "Adding authentication token to request")
                    connection.setRequestProperty("Authorization", "Bearer $token")
                } else {
                    if (DEBUG) Log.d(TAG, "No authentication token available")
                }
                
                // Enable strict transport security for HTTPS connections
                if (connection is HttpsURLConnection) {
                    connection.sslSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory()
                    connection.hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()
                }
                
                // TEMPORARY DEBUG LOG - REMOVE IN PRODUCTION
                val responseCode = connection.responseCode
                if (DEBUG) Log.d(TAG, "Response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    
                    // Check for JSON response before parsing
                    val responseStr = response.toString()
                    
                    // TEMPORARY DEBUG LOG - REMOVE IN PRODUCTION - Log only success, not content
                    if (DEBUG) Log.d(TAG, "Request successful, received data")
                    
                    if (!responseStr.startsWith("{") && !responseStr.startsWith("[")) {
                        return@withContext Result.failure(Exception("Invalid JSON response"))
                    }
                    
                    val jsonResponse = JSONObject(responseStr)
                    return@withContext Result.success(jsonResponse)
                } else {
                    // Read error stream if available
                    val errorStream = connection.errorStream
                    if (errorStream != null) {
                        val reader = BufferedReader(InputStreamReader(errorStream))
                        val error = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            error.append(line)
                        }
                        reader.close()
                        
                        // TEMPORARY DEBUG LOG - REMOVE IN PRODUCTION - Only log error code, not full details
                        if (DEBUG) Log.d(TAG, "HTTP Error $responseCode received from server")
                        
                        return@withContext Result.failure(Exception("HTTP Error $responseCode: ${error}"))
                    } else {
                        // TEMPORARY DEBUG LOG - REMOVE IN PRODUCTION
                        if (DEBUG) Log.d(TAG, "HTTP Error $responseCode with no error stream")
                        
                        return@withContext Result.failure(Exception("HTTP Error: $responseCode"))
                    }
                }
            } catch (e: Exception) {
                // TEMPORARY DEBUG LOG - REMOVE IN PRODUCTION - Log exception type but not full stack trace/message
                if (DEBUG) Log.d(TAG, "Network exception: ${e.javaClass.simpleName}")
                
                // Security: Error handling without logging sensitive information
                return@withContext Result.failure(e)
            } finally {
                connection?.disconnect()
            }
        }
    }
    
    /**
     * Sanitizes input parameters to prevent injection attacks
     */
    private fun sanitizeInput(input: String): String {
        return input.replace(Regex("[<>()\\[\\]&'\";]"), "")
    }
    

}
