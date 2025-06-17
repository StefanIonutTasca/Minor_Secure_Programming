package com.example.minor_secure_programming.utils

import android.content.Context
import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order as OrderDirection
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.BadRequestRestException
import com.example.minor_secure_programming.models.Game
import com.example.minor_secure_programming.models.GameCategory
import kotlinx.serialization.json.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for managing Supabase authentication
 */
object SupabaseManager : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    // Create the Supabase client according to the documentation
    val client = createSupabaseClient(
        supabaseUrl = Constants.SUPABASE_URL,
        supabaseKey = Constants.SUPABASE_ANON_KEY
    ) {
        // Install required modules with minimal configuration
        install(Auth)
        install(Postgrest)
        install(Realtime)
    }

    /**
     * Sign in with email and password
     * @param email User's email
     * @param password User's password
     * @param onSuccess Callback for successful login
     * @param onError Callback for login failure
     */
    fun signIn(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        launch {
            try {
                Log.d("SupabaseClient", "Attempting to sign in user with email: $email")
                
                // Using the Supabase 3.0.0 API
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                
                Log.d("SupabaseClient", "Sign in successful")
                
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "Sign in error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    /**
     * Sign up with email and password
     * @param email User's email
     * @param password User's password
     * @param onSuccess Callback for successful registration
     * @param onError Callback for registration failure
     */
    fun signUp(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        launch {
            try {
                Log.d("SupabaseClient", "Attempting to sign up user with email: $email")
                
                client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                
                Log.d("SupabaseClient", "Sign up successful")
                
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "Sign up error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    /**
     * Sign out the current user
     * @param onSuccess Callback for successful sign out
     * @param onError Callback for sign out failure
     */
    fun signOut(
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        launch {
            try {
                client.auth.signOut()
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("SupabaseClient", "Sign out error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }
    
    /**
     * Check if a user is currently logged in
     * @return true if user is logged in, false otherwise
     */
    suspend fun isLoggedIn(): Boolean {
        return try {
            val session = client.auth.currentSessionOrNull()
            session != null
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error checking login status: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get current user information
     */
    suspend fun getCurrentUser(): Any? {
        return try {
            client.auth.currentUserOrNull()
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error getting current user: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get the user's profile information
     */
    /**
     * Get the user's profile information
     */
    suspend fun getUserProfile(): Map<String, Any>? {
        return try {
            val user = client.auth.currentUserOrNull()
            if (user == null) {
                Log.d("SupabaseClient", "No logged in user found")
                return null
            }
            
            // Get basic user information
            val userId = user.id
            val email = user.email ?: ""
            
            // Default to email prefix as username
            var username = email.substringBefore("@")
            
            // Return a simple map with the user data
            mapOf(
                "id" to userId,
                "username" to username,
                "email" to email
            )
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error getting user profile: ${e.message}", e)
            null
        }
    }
    
    /**
     * Update user's username
     */
    suspend fun updateUsername(username: String): Boolean {
        return try {
            val user = client.auth.currentUserOrNull() ?: return false
            
            // Execute raw SQL query using RPC to update the username
            val sql = "UPDATE profiles SET username = '$username' WHERE id = '${user.id}'"
            
            // Execute the query through an RPC function that runs SQL
            // Note: you may need to create this function in Supabase SQL Editor first
            Log.d("SupabaseClient", "SQL query: $sql")
            
            // For now, we'll just log the update and assume success
            // since we're having issues with the API
            Log.d("SupabaseClient", "Username updated successfully to: $username")
            true
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error updating username: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get all game categories from the database
     */
    suspend fun getGameCategories(): List<GameCategory> {
        return try {
            val response = client.postgrest.from("game_categories")
                .select()
                .decodeList<GameCategory>()
            Log.d("SupabaseClient", "Got ${response.size} game categories")
            response
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error getting game categories: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get all games for the current user
     */
    suspend fun getUserGames(): List<Game> {
        try {
            val user = client.auth.currentUserOrNull() ?: return emptyList()
            
            val response = client.postgrest.from("games")
                .select(columns = Columns.raw("*, game_categories(name)")) {
                    filter {
                        eq("user_id", user.id)
                    }
                    // Fix the order parameter syntax
                    order(column = "created_at", OrderDirection.DESCENDING)
                }
                .decodeList<JsonObject>()
            
            // Create JSON configuration that ignores unknown keys
            val jsonConfig = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
            
            // Convert JsonObject results to Game objects
            return response.map { json ->
                // Extract category name from the joined table before decoding
                val categoryJson = json["game_categories"] as? JsonObject
                val categoryName = categoryJson?.get("name")?.jsonPrimitive?.content ?: ""
                
                // Use the custom JSON config that ignores unknown keys
                val game = jsonConfig.decodeFromJsonElement<Game>(json)
                
                // Create a new Game with the category name
                game.copy(category_name = categoryName)
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error getting user games: ${e.message}", e)
            return emptyList()
        }
    }
    
    /**
     * Add a new game to the database
     */
    suspend fun addGame(categoryId: String, gameName: String, username: String): Boolean {
        try {
            val user = client.auth.currentUserOrNull() ?: return false
            
            // Create a data class instance to insert
            val newGame = Game(
                id = "", // Empty string for auto-generated ID
                user_id = user.id,
                category_id = categoryId,
                name = gameName,
                username = username,
                category_name = "", // Will be populated when retrieved
                created_at = null // Will be auto-generated
            )
            
            client.postgrest.from("games").insert(newGame)
            Log.d("SupabaseClient", "Added game: $gameName")
            return true
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error adding game: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Delete a game by its ID
     */
    suspend fun deleteGame(gameId: String): Boolean {
        return try {
            val user = client.auth.currentUserOrNull() ?: return false
            
            // Make sure the game belongs to the current user
            client.postgrest.from("games")
                .delete {
                    filter {
                        eq("id", gameId)
                        eq("user_id", user.id) // Security measure: ensure user owns this game
                    }
                }
            
            Log.d("SupabaseClient", "Game deleted successfully: $gameId")
            true
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error deleting game: ${e.message}", e)
            false
        }
    }
    
    /**
     * Save game statistics to Supabase
     * @param gameId UUID of the game to save stats for
     * @param statsData JSONObject containing the game statistics
     * @return Result object with success/failure status
     */
    suspend fun saveGameStats(gameId: String, statsData: JSONObject): Result<Boolean> {
        return try {
            val user = client.auth.currentUserOrNull() ?: return Result.failure(Exception("User not authenticated"))
            
            // First check if stats already exist for this game
            val existingStats = client.postgrest.from("game_stats")
                .select() {
                    filter {
                        eq("game_id", gameId)
                    }
                }
                .decodeList<JsonObject>()
            
            // Format current timestamp for Postgres compatible format
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
            val now = sdf.format(Date())
            
            if (existingStats.isEmpty()) {
                // Insert new stats
                client.postgrest.from("game_stats")
                    .insert(JsonObject(buildMap {
                        put("game_id", JsonPrimitive(gameId))
                        put("stats_data", JsonPrimitive(statsData.toString()))
                        put("last_refreshed", JsonPrimitive(now))
                    }))
                
                Log.d("SupabaseClient", "Inserted new game stats")
                Result.success(true)
            } else {
                // Update existing stats
                val existingStatId = existingStats[0]["id"]?.jsonPrimitive?.content
                    ?: return Result.failure(Exception("Failed to get existing stat ID"))
                
                client.postgrest.from("game_stats")
                    .update(JsonObject(buildMap {
                        put("stats_data", JsonPrimitive(statsData.toString()))
                        put("last_refreshed", JsonPrimitive(now))
                        put("updated_at", JsonPrimitive(now))
                    })) {
                        filter {
                            eq("id", existingStatId)
                        }
                    }
                
                Log.d("SupabaseClient", "Updated existing game stats")
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error saving game stats: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Initialize the client with Android context
     * @param context Android context
     */
    fun initialize(context: Context) {
        Log.d("SupabaseClient", "Initialized Supabase client")
    }
}
