package com.example.minor_secure_programming.api

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service for making API calls to our backend
 */
class ApiService(private val context: Context) {
    
    companion object {
        // Local backend URL for emulator testing (10.0.2.2 is localhost from Android emulator)
        private const val LOCAL_URL = "http://10.0.2.2:8000/api/v1"
        // Hosted backend URL for production
        private const val HOSTED_URL = "https://minor-secure-programming-backend.onrender.com/api/v1"
        // Set to false to use local backend, true to use hosted backend
        private const val USE_HOSTED_URL = false
        // Base URL to be used based on the environment setting
        private val BASE_URL = if (USE_HOSTED_URL) HOSTED_URL else LOCAL_URL
        private const val TAG = "ApiService"
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
        return makeGetRequest("$BASE_URL/overwatch/players/$battletag")
    }
    
    /**
     * Get Overwatch player competitive rankings
     */
    suspend fun getOverwatchPlayerCompetitive(battletag: String): Result<JSONObject> {
        return makeGetRequest("$BASE_URL/overwatch/players/$battletag/competitive")
    }
    
    /**
     * Get Overwatch player summary statistics
     */
    suspend fun getOverwatchPlayerSummary(battletag: String): Result<JSONObject> {
        return makeGetRequest("$BASE_URL/overwatch/players/$battletag/summary")
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
        return makeGetRequest("$BASE_URL/overwatch/profile/$battletag")
    }
    
    /**
     * Generic method to make a GET request to the API
     */
    private suspend fun makeGetRequest(urlString: String): Result<JSONObject> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
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
                    
                    val jsonResponse = JSONObject(response.toString())
                    Result.success(jsonResponse)
                } else {
                    Log.e(TAG, "HTTP error code: $responseCode")
                    Result.failure(Exception("HTTP error: $responseCode"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}
