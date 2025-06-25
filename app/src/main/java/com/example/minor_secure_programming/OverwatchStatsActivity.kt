package com.example.minor_secure_programming

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.minor_secure_programming.api.ApiService
import com.example.minor_secure_programming.utils.SupabaseManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONArray
import java.text.NumberFormat
import java.util.Locale

class OverwatchStatsActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var sharedPreferences: SharedPreferences
    
    // Game data
    private var gameUsername: String = ""
    private var gameId: String? = null
    
    // Constant for SharedPreferences
    companion object {
        private const val PREFS_NAME = "OverwatchPrefs"
        private const val KEY_LAST_COMPARED_BATTLETAG = "last_compared_battletag"
        private const val KEY_LAST_COMPARED_NAME = "last_compared_name"
    }
    
    // Last compared player data
    // Keep track of last 5 searched players
    private val lastSearchedPlayers = mutableListOf<Pair<String?, String?>>() // List of (battletag, name) pairs
    private val maxSearchHistory = 5 // Store up to 5 recent players
    private var lastComparedPlayerData: JSONObject? = null
    
    // UI Components
    private lateinit var btnSearchFriend: Button
    private lateinit var btnRemove: Button
    private lateinit var btnCompare: Button
    
    private lateinit var cardPlayerStats: CardView
    private lateinit var cardComparison: CardView
    private lateinit var cardFriends: CardView
    
    private lateinit var imgPlayerAvatar: ImageView
    private lateinit var tvPlayerName: TextView
    private lateinit var tvPlayerLevel: TextView
    private lateinit var tvEndorsement: TextView
    
    private lateinit var tvGamesPlayed: TextView
    private lateinit var tvGamesWon: TextView
    private lateinit var tvGamesLost: TextView
    private lateinit var tvWinPercentage: TextView
    private lateinit var tvEliminations: TextView
    private lateinit var tvDeaths: TextView
    private lateinit var tvDamageDone: TextView
    private lateinit var tvTimePlayed: TextView
    
    private lateinit var spinnerProPlayers: Spinner
    private lateinit var layoutComparisonResults: LinearLayout
    
    // Friend search and comparison
    private lateinit var etFriendBattletag: TextInputEditText
    private lateinit var containerFriends: LinearLayout
    private lateinit var tvNoFriends: TextView
    
    // Pro player list for comparison
    private val proPlayers = listOf(
        "WarDevil-11626", // Requested pro player
        "Bjorn-12708",   // Super tank player
        "Violet-31431",   // Top support player
        "Jake-1311",      // Professional OWL player
        "Felix-11257"     // Player from the docs
    )
    
    // Currently loaded player data
    private var currentPlayerData: JSONObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overwatch_stats)
        
        // Initialize API service
        apiService = ApiService(this)
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        
        // Initialize UI components
        initializeUI()
        
        // Setup navigation listeners
        setupNavigationListeners()
        
        // Set up button click listeners
        setupButtonClickListeners()
        
        // Set up pro player spinner
        setupProPlayerSpinner()
        
        // Load last compared player from preferences
        loadLastComparedPlayer()
        
        // Get game data from intent
        gameUsername = intent.getStringExtra("USERNAME") ?: ""
        gameId = intent.getStringExtra("GAME_ID")
        
        // Set remove button action
        btnRemove.setOnClickListener {
            showRemoveConfirmationDialog()
        }
        
        // Automatically load player stats if we have a username
        if (gameUsername.isNotEmpty()) {
            // Format username for Overwatch API (replace # with -)
            val formattedUsername = gameUsername.replace("#", "-")
            // Auto-load player stats
            searchPlayerForComparison(formattedUsername, asMainPlayer = true)
        }
    }
    
    private fun initializeUI() {
        // Buttons
        btnRemove = findViewById(R.id.btn_remove_overwatch)
        btnCompare = findViewById(R.id.btn_compare)
        
        // Last compared player button
        val btnLastCompared = findViewById<Button>(R.id.btn_compare_last)
        btnLastCompared?.setOnClickListener {
            if (currentPlayerData != null && lastComparedPlayerData != null && lastSearchedPlayers.isNotEmpty()) {
                showComparisonDialog(currentPlayerData!!, lastComparedPlayerData!!, lastSearchedPlayers[0].second!!)
            } else {
                Toast.makeText(this, "Cannot compare: Missing player data", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Player info views
        imgPlayerAvatar = findViewById(R.id.img_player_avatar)
        tvPlayerName = findViewById(R.id.tv_player_name)
        tvPlayerLevel = findViewById(R.id.tv_player_level)
        tvEndorsement = findViewById(R.id.tv_endorsement)
        
        // Stats views
        tvGamesPlayed = findViewById(R.id.tv_qp_games_played)
        tvGamesWon = findViewById(R.id.tv_qp_games_won)
        tvGamesLost = findViewById(R.id.tv_qp_games_lost)
        tvWinPercentage = findViewById(R.id.tv_qp_win_percentage)
        tvEliminations = findViewById(R.id.tv_qp_eliminations)
        tvDeaths = findViewById(R.id.tv_qp_deaths)
        tvDamageDone = findViewById(R.id.tv_qp_damage_done)
        tvTimePlayed = findViewById(R.id.tv_qp_time_played)
        
        // Input field for battletag is now just the friend search field
        
        // Cards for displaying sections
        cardPlayerStats = findViewById(R.id.card_player_stats)
        cardComparison = findViewById(R.id.card_comparison)
        cardFriends = findViewById(R.id.card_friends)
        
        // Pro player selection spinner
        spinnerProPlayers = findViewById(R.id.spinner_pro_players)
        
        // Layout for comparison results
        layoutComparisonResults = findViewById(R.id.layout_comparison_results)
        
        // Friends search and comparison
        containerFriends = findViewById(R.id.container_friends)
        tvNoFriends = findViewById(R.id.tv_no_friends)
        
        // Initialize comparison button as disabled until we have player data
        btnCompare.isEnabled = false
    }
    
    /**
     * Set up navigation listeners
     */
    private fun setupNavigationListeners() {
        try {
            // Setup bottom navigation if it exists
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
            if (bottomNav != null) {
                bottomNav.setOnItemSelectedListener { item ->
                    when(item.itemId) {
                        R.id.navigation_home -> {
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            true
                        }
                        R.id.navigation_wellness -> {
                            // Already on stats page
                            true
                        }
                        R.id.navigation_cv -> {
                            val intent = Intent(this, CVActivity::class.java)
                            startActivity(intent)
                            true
                        }
                        else -> false
                    }
                }
                
                // Set active navigation item
                bottomNav.selectedItemId = R.id.navigation_wellness
            } else {
                // Bottom navigation is not in this layout, log and continue
                // Security: Logging removed
            }
        } catch (e: Exception) {
            // Security: Error handling - logging removed
        }
    }
    
    /**
     * Set up all button click listeners
     */
    private fun setupButtonClickListeners() {
        // Compare with pro button
        btnCompare.setOnClickListener {
            val selectedProPlayer = spinnerProPlayers.selectedItem as String
            compareWithPlayer(selectedProPlayer, if (selectedProPlayer == "WarDevil-11626") "WarDevil (Pro)" else selectedProPlayer)
        }
        
        // Friend search button - now handles both main player search and comparisons
        val btnSearchFriend = findViewById<Button>(R.id.btn_search_friend)
        btnSearchFriend.setOnClickListener {
            val etFriendBattletag = findViewById<TextInputEditText>(R.id.et_friend_battletag)
            val battletag = etFriendBattletag.text.toString().trim()
            
            // Validate the battletag before proceeding
            val validation = validateBattletag(battletag)
            if (validation.first) {
                val formattedBattletag = validation.second
                val asMainPlayer = currentPlayerData == null
                searchPlayerForComparison(formattedBattletag, asMainPlayer)
            } else {
                Toast.makeText(this, validation.second, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Validate an Overwatch battletag for security and correctness
     * @param battletag The raw battletag input from the user
     * @return Pair<Boolean, String> where first is whether it's valid, second is error message or formatted battletag
     */
    private fun validateBattletag(battletag: String): Pair<Boolean, String> {
        val trimmed = battletag.trim()
        
        // Check if empty
        if (trimmed.isEmpty()) {
            return Pair(false, "Please enter a battletag")
        }
        
        // Check for minimum length (name + # + numbers)
        if (trimmed.length < 3) {
            return Pair(false, "Battletag is too short")
        }
        
        // Check for maximum length to prevent DoS attacks
        if (trimmed.length > 32) {
            return Pair(false, "Battletag is too long")
        }
        
        // Check for valid format: either with # or - separator
        val hasCorrectFormat = trimmed.contains("#") || trimmed.contains("-")
        if (!hasCorrectFormat) {
            return Pair(false, "Invalid format. Use Name#1234 or Name-1234")
        }
        
        // Check for potentially harmful characters
        val sanitized = trimmed.replace("[<>()\\[\\]&'\";]".toRegex(), "")
        if (sanitized != trimmed) {
            return Pair(false, "Battletag contains invalid characters")
        }
        
        // Format appropriately (Overwatch API expects dash, not hashtag)
        val formattedBattletag = trimmed.replace("#", "-")
        return Pair(true, formattedBattletag)
    }
    
    /**
     * Load last compared player from SharedPreferences
     * Also attempts to restore the saved JSON data if available
     */
    private fun loadLastComparedPlayer() {
        try {
            // Load player history
            val savedHistory = sharedPreferences.getString("player_search_history", null)
            if (savedHistory != null) {
                val players = savedHistory.split(",")
                for (player in players) {
                    val parts = player.split(":")
                    if (parts.size == 2) {
                        lastSearchedPlayers.add(Pair(parts[0], parts[1]))
                    }
                }
            }
            
            // Also try to load the last compared data JSON
            val savedData = sharedPreferences.getString("last_compared_data", null)
            if (savedData != null) {
                try {
                    lastComparedPlayerData = JSONObject(savedData)
                } catch (e: Exception) {
                    // Security: Error handling - logging removed
                }
            }
            
            if (lastSearchedPlayers.isNotEmpty()) {
                // Security: Logging removed
                updateLastComparedPlayerUI()
            } else {
                // Security: Logging removed
            }
        } catch (e: Exception) {
            // Security: Error handling - logging removed
        }
    }
    
    /**
     * Save last compared player data to SharedPreferences
     */
    private fun saveLastComparedPlayer(battletag: String?, playerName: String?, playerData: JSONObject? = null) {
        try {
            // Security: Logging removed
            
            // Don't add duplicates - if this player is already in history, remove it first
            lastSearchedPlayers.removeIf { it.first == battletag }
            
            // Add to the beginning of the list (most recent)
            lastSearchedPlayers.add(0, Pair(battletag, playerName))
            
            // Keep only the last maxSearchHistory items
            while (lastSearchedPlayers.size > maxSearchHistory) {
                lastSearchedPlayers.removeAt(lastSearchedPlayers.size - 1)
            }
            
            // Save the latest player data
            if (playerData != null) {
                lastComparedPlayerData = playerData
            }
            
            // Save to shared preferences
            val editor = sharedPreferences.edit()
            
            // Save the history as a string (battletag;name,battletag;name,...)
            val history = lastSearchedPlayers.joinToString(",") { "${it.first ?: ""}:${it.second ?: ""}" }
            editor.putString("player_search_history", history)
            
            // Also save the full JSON data of the most recent comparison
            if (playerData != null) {
                editor.putString("last_compared_data", playerData.toString())
            }
            
            editor.apply()
            
            // Update UI to show the last compared player
            updateLastComparedPlayerUI()
            
            // Security: Logging removed
        } catch (e: Exception) {
            // Security: Error handling - logging removed
        }
    }
    
    /**
     * Update the UI to show the search history in the friends section
     */
    private fun updateLastComparedPlayerUI() {
        if (lastSearchedPlayers.isEmpty()) {
            // No history yet
            tvNoFriends.visibility = View.VISIBLE
            return
        }
        
        // Remove "no friends" text if present
        tvNoFriends.visibility = View.GONE
        
        // Clear existing views
        containerFriends.removeAllViews()
        
        // Create layout inflater
        val inflater = LayoutInflater.from(this)
        
        // Let's add the WarDevil pro player first - ONLY ONCE
        val proTag = "WarDevil-11626"
        val proName = "WarDevil (Pro)"
        
        val proView = inflater.inflate(R.layout.item_overwatch_friend, containerFriends, false)
        proView.tag = "friend_$proTag"
        
        val proNameView = proView.findViewById<TextView>(R.id.tv_friend_title)
        val proBattletagView = proView.findViewById<TextView>(R.id.tv_friend_battletag)
        val proCompareBtn = proView.findViewById<Button>(R.id.btn_friend_compare)
        
        proNameView.text = "WarDevil"
        proBattletagView.text = proTag
        proCompareBtn.setOnClickListener {
            if (currentPlayerData != null) {
                compareWithPlayer(proTag, proName)
            } else {
                Toast.makeText(this, "Load your own stats first", Toast.LENGTH_SHORT).show()
            }
        }
        
        containerFriends.addView(proView)
        
        // Show all players from regular search history (up to 5)
        // Filter out any WarDevil entries to avoid duplication
        for (playerPair in lastSearchedPlayers) {
            val battletag = playerPair.first
            val playerName = playerPair.second
            
            if (battletag == null || battletag == proTag) continue
            
            // Create new view for this player
            val friendView = inflater.inflate(R.layout.item_overwatch_friend, containerFriends, false)
            friendView.tag = "friend_$battletag"
            
            // Set up the view
            val tvName = friendView.findViewById<TextView>(R.id.tv_friend_title)
            val tvBattletag = friendView.findViewById<TextView>(R.id.tv_friend_battletag)
            val btnCompare = friendView.findViewById<Button>(R.id.btn_friend_compare)
            
            tvName.text = playerName ?: battletag
            tvBattletag.text = battletag
            btnCompare.setOnClickListener {
                if (currentPlayerData != null) {
                    // Compare with this player
                    compareWithPlayer(battletag, playerName)
                } else {
                    Toast.makeText(this, "Load your own stats first", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Add to container
            containerFriends.addView(friendView)
        }
    }
    
    /**
     * Search for a player for comparison or as main player
     * @param battletag The player battletag to search for
     * @param asMainPlayer If true, update the main player stats; otherwise treat as comparison only
     */
    private fun searchPlayerForComparison(battletag: String, asMainPlayer: Boolean = false) {
        // Security: Double-check input validation
        val validation = validateBattletag(battletag)
        if (!validation.first) {
            Toast.makeText(this, validation.second, Toast.LENGTH_SHORT).show()
            return
        }
        
        // Use validated battletag for the search
        val validatedBattletag = validation.second
        
        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setView(layoutInflater.inflate(R.layout.dialog_loading_overwatch, null))
            .setCancelable(false)
            .create()
            
        loadingDialog.show()
        
        // Call API to get player data
        lifecycleScope.launch {
            try {
                val result = apiService.getOverwatchCombinedProfile(battletag)
                
                // Dismiss loading dialog
                loadingDialog.dismiss()
                
                if (result.isSuccess) {
                    val response = result.getOrNull()
                    
                    if (response != null && response.optBoolean("success", false)) {
                        val data = response.optJSONObject("data")
                        if (data != null) {
                            // Security: Logging removed
                            
                            // Extract player name from profile
                            val profile = data.optJSONObject("profile")
                            val summary = profile?.optJSONObject("summary")
                            val playerName = summary?.optString("username", "Unknown") ?: "Unknown"
                            
                            if (asMainPlayer) {
                                // Use as main player
                                currentPlayerData = data
                                
                                // Update UI with player data
                                displayPlayerData(data)
                                
                                // Enable comparison button now that we have data
                                btnCompare.isEnabled = true
                                
                                // Show cards that were previously hidden
                                cardPlayerStats.visibility = View.VISIBLE
                                cardComparison.visibility = View.VISIBLE
                                cardFriends.visibility = View.VISIBLE
                                
                                Toast.makeText(this@OverwatchStatsActivity, "Player stats loaded successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                // Show alert dialog with option to compare
                                AlertDialog.Builder(this@OverwatchStatsActivity)
                                    .setTitle("Player Found")
                                    .setMessage("Player $playerName was found. Do you want to compare stats?")
                                    .setPositiveButton("Compare") { _, _ ->
                                        // Save as last compared player
                                        saveLastComparedPlayer(battletag, playerName, data)
                                        
                                        // Show comparison
                                        if (currentPlayerData != null) {
                                            showComparisonDialog(currentPlayerData!!, data, playerName)
                                        } else {
                                            Toast.makeText(this@OverwatchStatsActivity, "Load your own stats first before comparing", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .setNegativeButton("Cancel", null)
                                    .show()
                            }
                        } else {
                            showError("Invalid response format - no data found")
                        }
                    } else {
                        val errorMessage = response?.optString("detail") ?: "Unknown error"
                        showError("API error: $errorMessage")
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    showError("Request failed: $error")
                }
            } catch (e: Exception) {
                loadingDialog.dismiss()
                // Security: Error handling - logging removed
                showError("Error: ${e.message}")
            }
        }
    }
    
    /**
     * Load and compare with player by battletag (Pro Player implementation)
     */
    /**
     * Compare with any player by battletag
     * This is a unified method that handles all comparisons including WarDevil
     */
    private fun compareWithPlayer(battletag: String, displayName: String? = null) {
        if (currentPlayerData == null) {
            Toast.makeText(this, "You need to load your profile first", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Special case for WarDevil pro player
        val playerName = if (battletag == "WarDevil-11626" && displayName == null) {
            "WarDevil (Pro)"
        } else {
            displayName ?: battletag
        }
        
        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setView(layoutInflater.inflate(R.layout.dialog_loading_overwatch, null))
            .setCancelable(false)
            .create()
        
        loadingDialog.show()
        
        lifecycleScope.launch {
            try {
                val result = apiService.getOverwatchCombinedProfile(battletag)
                
                // Dismiss loading dialog
                loadingDialog.dismiss()
                
                if (result.isSuccess) {
                    val response = result.getOrNull()
                    
                    if (response != null && response.optBoolean("success", false)) {
                        val data = response.optJSONObject("data")
                        if (data != null) {
                            // Extract player name if not provided
                            val finalPlayerName = if (displayName == null && battletag != "WarDevil-11626") {
                                val profile = data.optJSONObject("profile")
                                val summary = profile?.optJSONObject("summary")
                                summary?.optString("username", battletag) ?: battletag
                            } else {
                                playerName
                            }
                            
                            // Save this data for future comparisons
                            lastComparedPlayerData = data
                            
                            // Only save to search history if it's not a pro player
                            if (battletag != "WarDevil-11626") {
                                saveLastComparedPlayer(battletag, finalPlayerName, data)
                            }
                            
                            // Show comparison
                            if (currentPlayerData != null) {
                                showComparisonDialog(currentPlayerData!!, data, finalPlayerName)
                            } else {
                                Toast.makeText(this@OverwatchStatsActivity, "Load your own stats first before comparing", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            showError("Invalid response format - no data found")
                        }
                    } else {
                        val errorMessage = response?.optString("detail") ?: "Unknown error"
                        showError("API error: $errorMessage")
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    showError("Request failed: $error")
                }
            } catch (e: Exception) {
                loadingDialog.dismiss()
                // Security: Error handling - logging removed
                showError("Error: ${e.message}")
            }
        }
    }
    
    // searchPlayer method is defined later in the file
    
    // Using compareWithProPlayer instead
    
    private fun setupProPlayerSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, proPlayers)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProPlayers.adapter = adapter
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        // Security: Error handling - logging removed
    }
    
    private fun searchPlayer(battletag: String) {
        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setView(layoutInflater.inflate(R.layout.dialog_loading_overwatch, null))
            .setCancelable(false)
            .create()
        
        loadingDialog.show()
        
        // Call API to get player data
        lifecycleScope.launch {
            try {
                val result = apiService.getOverwatchCombinedProfile(battletag)
                
                // Dismiss loading dialog
                loadingDialog.dismiss()
                
                if (result.isSuccess) {
                    val response = result.getOrNull()
                    // Security: Logging removed
                    
                    if (response != null && response.optBoolean("success", false)) {
                        val data = response.optJSONObject("data")
                        if (data != null) {
                            // Security: Logging removed
                            
                            // Store current player data for later comparison
                            currentPlayerData = data
                            
                            // Extract player name from profile for possible future comparisons
                            val profile = data.optJSONObject("profile")
                            val summary = profile?.optJSONObject("summary")
                            val playerName = summary?.optString("username", battletag) ?: battletag
                            
                            // Display player data
                            displayPlayerData(data)
                            
                            // Enable compare button
                            btnCompare.isEnabled = true
                            
                            // Save to Supabase if we have a game ID
                            gameId?.let { id ->
                                // Show saving indicator
                                val savingSnackbar = Snackbar.make(
                                    findViewById(android.R.id.content),
                                    "Saving player stats to your profile...",
                                    Snackbar.LENGTH_INDEFINITE
                                ).apply { show() }
                                
                                // Use lifecycleScope for Kotlin coroutines
                                lifecycleScope.launch {
                                    try {
                                        // Save the stats data to Supabase
                                        val statsResult = SupabaseManager.saveGameStats(id, data)
                                        savingSnackbar.dismiss()
                                        
                                        if (statsResult.isSuccess) {
                                            Snackbar.make(
                                                findViewById(android.R.id.content),
                                                "Player stats saved successfully!",
                                                Snackbar.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            val error = statsResult.exceptionOrNull()
                                            // Security: Error handling - logging removed
                                            Snackbar.make(
                                                findViewById(android.R.id.content),
                                                "Profile displayed but couldn't save stats: ${error?.message ?: "Unknown error"}",
                                                Snackbar.LENGTH_SHORT
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        savingSnackbar.dismiss()
                                        // Security: Error handling - logging removed
                                        Snackbar.make(
                                            findViewById(android.R.id.content),
                                            "Error saving stats: ${e.message ?: "Unknown error"}",
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } ?: run {
                                // No game ID available
                                // Security: Warning removed
                            }
                        } else {
                            showError("Invalid response format - no data object found")
                        }
                    } else {
                        val errorMessage = response?.optString("detail") ?: "Unknown error"
                        showError("API error: $errorMessage")
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    showError("Request failed: $error")
                }
            } catch (e: Exception) {
                loadingDialog.dismiss()
                // Security: Error handling - logging removed
                showError("Error: ${e.message}")
            }
        }
    }
    
    private fun displayPlayerData(data: JSONObject) {
        try {
            // Security: Logging removed
            
            // Extract profile data
            val profile = data.optJSONObject("profile")
            // Security: Logging removed
            
            if (profile != null) {
                // The player data is in a nested 'summary' object
                val summary = profile.optJSONObject("summary")
                // Security: Logging removed
                
                if (summary != null) {
                    val playerName = summary.optString("username", "Unknown")
                    
                    // Player level might not be directly available in the API response
                    // Fall back to calculating level from stats if needed
                    val playerLevel = summary.optInt("player_level", 0)
                    
                    // Endorsement is nested in an object
                    val endorsementObj = summary.optJSONObject("endorsement")
                    val endorsementLevel = endorsementObj?.optInt("level", 0) ?: 0
                    
                    // Security: Logging removed
                    
                    // Update UI
                    tvPlayerName.text = playerName
                    tvPlayerLevel.text = "Level: $playerLevel"
                    tvEndorsement.text = "Endorsement: $endorsementLevel"
                    
                    // TODO: Load avatar image using a library like Glide or Picasso
                    val avatarUrl = summary.optString("avatar", "")
                    // Security: Logging removed
                    // Glide.with(this).load(avatarUrl).into(imgPlayerAvatar)
                    
                    // No longer displaying competitive rankings
                    
                    // Handle stats
                    displayPlayerStats(profile)
                } else {
                    // Security: Warning removed
                    setDefaultPlayerInfo()
                    setDefaultRanks()
                    setDefaultStats()
                }
            } else {
                // Security: Warning removed
                setDefaultPlayerInfo()
                setDefaultRanks()
                setDefaultStats()
            }
        } catch (e: Exception) {
            // Security: Error handling - logging removed
            setDefaultPlayerInfo()
            setDefaultRanks()
            setDefaultStats()
        }
    }

private fun setDefaultPlayerInfo() {
    tvPlayerName.text = "Unknown"
    tvPlayerLevel.text = "Level: 0"
    tvEndorsement.text = "Endorsement: 0"
}

private fun setDefaultRanks() {
    // No longer displaying ranks
}

private fun setDefaultStats() {
    tvGamesPlayed.text = "Games Played: 0"
    tvGamesWon.text = "Wins: 0"
    tvGamesLost.text = "Losses: 0"
    tvWinPercentage.text = "Win %: 0"
    tvEliminations.text = "Eliminations: 0"
    tvDeaths.text = "Deaths: 0"
    tvDamageDone.text = "Damage Done: 0"
    tvTimePlayed.text = "Time Played: 0h 0m"
}

private fun displayCompetitiveRanks(summary: JSONObject) {
    // No longer displaying competitive ranks
    // Security: Logging removed
}

private fun getRankName(rankObj: JSONObject): String {
    // This method is retained for potential future use but not currently used
    val tier = rankObj.optInt("tier", 0)
    val division = rankObj.optInt("division", 0)
    
    // Convert tier (which is a number) to the rank name
    val tierName = when(tier) {
        1 -> "Bronze"
        2 -> "Silver"
        3 -> "Gold"
        4 -> "Platinum"
        5 -> "Diamond"
        6 -> "Master"
        7 -> "Grandmaster"
        else -> "Unknown"
    }
    
    return "$tierName $division"
}

private fun displayPlayerStats(profile: JSONObject) {
    try {
        val stats = profile.optJSONObject("stats")
        if (stats != null) {
            val pc = stats.optJSONObject("pc")
            if (pc != null) {
                val quickplay = pc.optJSONObject("quickplay")
                if (quickplay != null) {
                    val careerStats = quickplay.optJSONObject("career_stats")
                    if (careerStats != null) {
                        val allHeroes = careerStats.optJSONArray("all-heroes")
                        
                        // Initialize variables to store stats
                        var gamesPlayed = 0
                        var gamesWon = 0
                        var gamesLost = 0
                        var eliminations = 0
                        var deaths = 0
                        var damageDone = 0
                        var timePlayed = 0
                        
                        // Extract stats from the all-heroes category
                        if (allHeroes != null) {
                            // Go through each category (game, combat, etc.)
                            for (i in 0 until allHeroes.length()) {
                                val category = allHeroes.optJSONObject(i)
                                if (category != null) {
                                    val categoryName = category.optString("category", "")
                                    val stats = category.optJSONArray("stats")
                                    
                                    // Extract stats based on category
                                    if (stats != null) {
                                        when (categoryName) {
                                            "game" -> {
                                                // Find games played, won, and lost
                                                for (j in 0 until stats.length()) {
                                                    val stat = stats.optJSONObject(j)
                                                    val key = stat?.optString("key", "")
                                                    val value = stat?.optInt("value", 0) ?: 0
                                                    
                                                    when (key) {
                                                        "games_played" -> gamesPlayed = value
                                                        "games_won" -> gamesWon = value
                                                        "games_lost" -> gamesLost = value
                                                        "time_played" -> timePlayed = value
                                                    }
                                                }
                                            }
                                            "combat" -> {
                                                // Find eliminations, deaths, damage done
                                                for (j in 0 until stats.length()) {
                                                    val stat = stats.optJSONObject(j)
                                                    val key = stat?.optString("key", "")
                                                    val value = stat?.optInt("value", 0) ?: 0
                                                    
                                                    when (key) {
                                                        "eliminations" -> eliminations = value
                                                        "deaths" -> deaths = value
                                                        "hero_damage_done" -> damageDone = value
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Calculate win percentage
                            val winPercentage = if (gamesPlayed > 0) {
                                (gamesWon.toFloat() / gamesPlayed.toFloat() * 100).toInt()
                            } else {
                                0
                            }
                            
                            // Convert time played (in seconds) to hours and minutes
                            val hours = timePlayed / 3600
                            val minutes = (timePlayed % 3600) / 60
                            
                            // Format damage done for readability (e.g., 1,234,567)
                            val formattedDamage = NumberFormat.getNumberInstance(Locale.US).format(damageDone)
                            
                            // Update UI
                            tvGamesPlayed.text = "Games Played: $gamesPlayed"
                            tvGamesWon.text = "Wins: $gamesWon"
                            tvGamesLost.text = "Losses: $gamesLost"
                            tvWinPercentage.text = "Win %: $winPercentage%"
                            tvEliminations.text = "Eliminations: $eliminations"
                            tvDeaths.text = "Deaths: $deaths"
                            tvDamageDone.text = "Damage Done: $formattedDamage"
                            tvTimePlayed.text = "Time Played: ${hours}h ${minutes}m"
                            
                            // Security: Logging removed
                        } else {
                            // Security: Warning removed
                            setDefaultStats()
                        }
                    } else {
                        // Security: Warning removed
                        setDefaultStats()
                    }
                } else {
                    // Security: Warning removed
                    setDefaultStats()
                }
            } else {
                // Security: Warning removed
                setDefaultStats()
            }
        } else {
            // Security: Warning removed
            setDefaultStats()
        }
    } catch (e: Exception) {
        // Security: Error handling - logging removed
        setDefaultStats()
    }
    
    // Show player stats card
    cardPlayerStats.visibility = View.VISIBLE
}

    /**
     * Custom comparison dialog that shows player stats side-by-side with highlighting
     * Styled exactly like the Dota implementation
     */
    private fun showComparisonDialog(yourData: JSONObject, otherData: JSONObject, otherPlayerName: String) {
        try {
            // Create dialog with same layout as Dota comparison
            val dialogView = layoutInflater.inflate(R.layout.dialog_compare_stats, null)
            
            // Extract player names for display
            val playerNameSummary = yourData.optJSONObject("profile")?.optJSONObject("summary") 
            val yourName = playerNameSummary?.optString("name") ?: gameUsername
            
            // Set player names in the header exactly like Dota
            val tvPlayer1Name = dialogView.findViewById<TextView>(R.id.tvPlayer1Name)
            val tvPlayer2Name = dialogView.findViewById<TextView>(R.id.tvPlayer2Name)
            tvPlayer1Name.text = yourName
            tvPlayer2Name.text = otherPlayerName
            
            // Identify and style all section headers properly
            // Use direct view references to avoid API incompatibilities
            val allTextViews = ArrayList<View>()
            dialogView.findViewsWithText(allTextViews, "Wins", View.FIND_VIEWS_WITH_TEXT)
            dialogView.findViewsWithText(allTextViews, "Losses", View.FIND_VIEWS_WITH_TEXT)
            dialogView.findViewsWithText(allTextViews, "Win Rate", View.FIND_VIEWS_WITH_TEXT)
            dialogView.findViewsWithText(allTextViews, "Top Heroes", View.FIND_VIEWS_WITH_TEXT)
            
            // Apply styling to all headers found - use a lighter purple color
            // Use teal_200 which is a lighter and more pleasant color
            for (i in 0 until allTextViews.size) {
                val view = allTextViews[i]
                if (view is TextView) {
                    view.setTextColor(ContextCompat.getColor(this, R.color.teal_200))
                    view.setTypeface(null, android.graphics.Typeface.BOLD)
                }
            }
            
            // Add extensive logging to understand the JSON structure
            // Security: Logging removed
            // Security: Logging removed
            
            if (yourData.has("profile")) {
                // Security: Logging removed
                
                val summaryObj = yourData.optJSONObject("profile")?.optJSONObject("summary")
                if (summaryObj != null) {
                    // Security: Logging removed
                    // Security: Logging removed
                }
                
                val statsObj = yourData.optJSONObject("profile")?.optJSONObject("stats")
                if (statsObj != null) {
                    // Security: Logging removed
                    // Security: Logging removed
                }
            }
            
            // Security: Logging removed
            
            if (otherData.has("profile")) {
                // Security: Logging removed
                
                val otherSummaryObj = otherData.optJSONObject("profile")?.optJSONObject("summary")
                if (otherSummaryObj != null) {
                    // Security: Logging removed
                    // Security: Logging removed
                }
            }
            
            // Extract stats using the debug information to guide us
            // For Overwatch, the competitive stats are the main source of truth
            var yourGamesWon = 0
            var yourGamesPlayed = 0
            var otherGamesWon = 0
            var otherGamesPlayed = 0
            
            // Try multiple paths to find the information, from most specific to most general
            
            // Path 1: Get stats from heroes_comparisons which contains detailed hero stats
            // This is where the actual games_won data is stored based on the logs
            try {
                val pcStats = yourData.optJSONObject("profile")?.optJSONObject("stats")?.optJSONObject("pc")
                if (pcStats != null) {
                    val quickplay = pcStats.optJSONObject("quickplay")
                    if (quickplay != null) {
                        val heroesComparisons = quickplay.optJSONObject("heroes_comparisons")
                        if (heroesComparisons != null) {
                            // Get games won from all heroes
                            val gamesWonObj = heroesComparisons.optJSONObject("games_won")
                            if (gamesWonObj != null) {
                                val heroValues = gamesWonObj.optJSONArray("values")
                                if (heroValues != null) {
                                    // Sum up all wins across heroes
                                    for (i in 0 until heroValues.length()) {
                                        val heroStat = heroValues.optJSONObject(i)
                                        yourGamesWon += heroStat?.optInt("value", 0) ?: 0
                                    }
                                }
                            }
                            
                            // Calculate total games played using win percentage data
                            val winPercentageObj = heroesComparisons.optJSONObject("win_percentage")
                            if (winPercentageObj != null) {
                                // Estimate games played based on wins and win percentage
                                if (yourGamesWon > 0) {
                                    // Use average win percentage across heroes to estimate
                                    var totalWinPercentage = 0
                                    var heroCount = 0
                                    val percentValues = winPercentageObj.optJSONArray("values")
                                    if (percentValues != null) {
                                        for (i in 0 until percentValues.length()) {
                                            val percentData = percentValues.optJSONObject(i)
                                            val percent = percentData?.optInt("value", 0) ?: 0
                                            if (percent > 0) {
                                                totalWinPercentage += percent
                                                heroCount++
                                            }
                                        }
                                    }
                                    
                                    // Calculate estimated games played using average win percentage
                                    if (heroCount > 0) {
                                        val avgWinPercentage = totalWinPercentage.toFloat() / heroCount
                                        yourGamesPlayed = if (avgWinPercentage > 0) {
                                            (yourGamesWon * 100 / avgWinPercentage).toInt()
                                        } else {
                                            yourGamesWon
                                        }
                                    } else {
                                        yourGamesPlayed = yourGamesWon
                                    }
                                }
                            }
                            
                            // Security: Logging removed
                        }
                    }
                }
            } catch (e: Exception) {
                // Security: Error handling - logging removed
            }
            
            // Do the same for other player
            try {
                val pcStats = otherData.optJSONObject("profile")?.optJSONObject("stats")?.optJSONObject("pc")
                if (pcStats != null) {
                    val quickplay = pcStats.optJSONObject("quickplay")
                    if (quickplay != null) {
                        val heroesComparisons = quickplay.optJSONObject("heroes_comparisons")
                        if (heroesComparisons != null) {
                            // Get games won from all heroes
                            val gamesWonObj = heroesComparisons.optJSONObject("games_won")
                            if (gamesWonObj != null) {
                                val heroValues = gamesWonObj.optJSONArray("values")
                                if (heroValues != null) {
                                    // Sum up all wins across heroes
                                    for (i in 0 until heroValues.length()) {
                                        val heroStat = heroValues.optJSONObject(i)
                                        otherGamesWon += heroStat?.optInt("value", 0) ?: 0
                                    }
                                }
                            }
                            
                            // Calculate total games played using win percentage data
                            val winPercentageObj = heroesComparisons.optJSONObject("win_percentage")
                            if (winPercentageObj != null) {
                                // Estimate games played based on wins and win percentage
                                if (otherGamesWon > 0) {
                                    // Use average win percentage across heroes to estimate
                                    var totalWinPercentage = 0
                                    var heroCount = 0
                                    val percentValues = winPercentageObj.optJSONArray("values")
                                    if (percentValues != null) {
                                        for (i in 0 until percentValues.length()) {
                                            val percentData = percentValues.optJSONObject(i)
                                            val percent = percentData?.optInt("value", 0) ?: 0
                                            if (percent > 0) {
                                                totalWinPercentage += percent
                                                heroCount++
                                            }
                                        }
                                    }
                                    
                                    // Calculate estimated games played using average win percentage
                                    if (heroCount > 0) {
                                        val avgWinPercentage = totalWinPercentage.toFloat() / heroCount
                                        otherGamesPlayed = if (avgWinPercentage > 0) {
                                            (otherGamesWon * 100 / avgWinPercentage).toInt()
                                        } else {
                                            otherGamesWon
                                        }
                                    } else {
                                        otherGamesPlayed = otherGamesWon
                                    }
                                }
                            }
                            
                            // Security: Logging removed
                        }
                    }
                }
            } catch (e: Exception) {
                // Security: Error handling - logging removed
            }
            
            // Path 2: Competitive stats 
            if (yourGamesPlayed == 0 && yourData.has("competitive")) {
                val compStats = yourData.optJSONObject("competitive")
                if (compStats != null) {
                    // Security: Logging removed
                    yourGamesWon = compStats.optInt("wins", compStats.optInt("games_won", 0))
                    yourGamesPlayed = compStats.optInt("games", compStats.optInt("games_played", 0))
                    // Security: Logging removed
                }
            }
            
            if (otherGamesPlayed == 0 && otherData.has("competitive")) {
                val compStats = otherData.optJSONObject("competitive")
                if (compStats != null) {
                    // Security: Logging removed
                    otherGamesWon = compStats.optInt("wins", compStats.optInt("games_won", 0))
                    otherGamesPlayed = compStats.optInt("games", compStats.optInt("games_played", 0))
                    // Security: Logging removed
                }
            }
            
            // Path 3: Try profile.stats
            if (yourGamesPlayed == 0) {
                val profileStats = yourData.optJSONObject("profile")?.optJSONObject("stats")
                if (profileStats != null) {
                    // Security: Logging removed
                    yourGamesWon = profileStats.optInt("wins", profileStats.optInt("games_won", 0))
                    yourGamesPlayed = profileStats.optInt("games", profileStats.optInt("games_played", 0))
                    // Security: Logging removed
                }
            }
            
            if (otherGamesPlayed == 0) {
                val profileStats = otherData.optJSONObject("profile")?.optJSONObject("stats")
                if (profileStats != null) {
                    // Security: Logging removed
                    otherGamesWon = profileStats.optInt("wins", profileStats.optInt("games_won", 0))
                    otherGamesPlayed = profileStats.optInt("games", profileStats.optInt("games_played", 0))
                    // Security: Logging removed
                }
            }
            
            // Path 4: Try summary data
            if (yourGamesPlayed == 0) {
                val summary = yourData.optJSONObject("profile")?.optJSONObject("summary")
                if (summary != null) {
                    yourGamesWon = summary.optInt("wins", summary.optInt("games_won", 0))
                    yourGamesPlayed = summary.optInt("games", summary.optInt("games_played", 0))
                    // Security: Logging removed
                }
            }
            
            if (otherGamesPlayed == 0) {
                val summary = otherData.optJSONObject("profile")?.optJSONObject("summary")
                if (summary != null) {
                    otherGamesWon = summary.optInt("wins", summary.optInt("games_won", 0))
                    otherGamesPlayed = summary.optInt("games", summary.optInt("games_played", 0))
                    // Security: Logging removed
                }
            }
            
            // Fallback option - look for a game_stats object that might contain this data
            if (yourGamesPlayed == 0 && yourData.has("game_stats")) {
                val gameStats = yourData.optJSONObject("game_stats")
                if (gameStats != null) {
                    yourGamesWon = gameStats.optInt("games_won", 0)
                    yourGamesPlayed = gameStats.optInt("games_played", 0)
                    // Security: Logging removed
                }
            }
            
            if (otherGamesPlayed == 0 && otherData.has("game_stats")) {
                val gameStats = otherData.optJSONObject("game_stats")
                if (gameStats != null) {
                    otherGamesWon = gameStats.optInt("games_won", 0)
                    otherGamesPlayed = gameStats.optInt("games_played", 0)
                    // Security: Logging removed
                }
            }
            
            // Do NOT use test values - we have real data now
            // Security: Logging removed
            
            // Calculate win rates
            val yourWinRate = if (yourGamesPlayed > 0) (yourGamesWon.toFloat() / yourGamesPlayed * 100) else 0f
            val otherWinRate = if (otherGamesPlayed > 0) (otherGamesWon.toFloat() / otherGamesPlayed * 100) else 0f
            
            // Security: Logging removed
            
            // Populate win stats
            val tvPlayer1Wins = dialogView.findViewById<TextView>(R.id.tvPlayer1Wins)
            val tvPlayer2Wins = dialogView.findViewById<TextView>(R.id.tvPlayer2Wins)
            tvPlayer1Wins.text = yourGamesWon.toString()
            tvPlayer2Wins.text = otherGamesWon.toString()
            highlightBetterStat(tvPlayer1Wins, tvPlayer2Wins, true)
            
            // Losses (games played - games won)
            val yourLosses = yourGamesPlayed - yourGamesWon
            val otherLosses = otherGamesPlayed - otherGamesWon
            val tvPlayer1Losses = dialogView.findViewById<TextView>(R.id.tvPlayer1Losses)
            val tvPlayer2Losses = dialogView.findViewById<TextView>(R.id.tvPlayer2Losses)
            tvPlayer1Losses.text = yourLosses.toString()
            tvPlayer2Losses.text = otherLosses.toString()
            highlightBetterStat(tvPlayer1Losses, tvPlayer2Losses, false) // Lower is better for losses
            
            // Win rate
            val tvPlayer1WinRate = dialogView.findViewById<TextView>(R.id.tvPlayer1WinRate)
            val tvPlayer2WinRate = dialogView.findViewById<TextView>(R.id.tvPlayer2WinRate)
            tvPlayer1WinRate.text = String.format("%.1f%%", yourWinRate)
            tvPlayer2WinRate.text = String.format("%.1f%%", otherWinRate)
            highlightBetterStat(tvPlayer1WinRate, tvPlayer2WinRate, true)
            
            // Populate hero stats (roles/heroes in Overwatch)
            val yourRoles = extractTopRoles(yourData)
            val otherRoles = extractTopRoles(otherData)
            val tvPlayer1Heroes = dialogView.findViewById<TextView>(R.id.tvPlayer1Heroes)
            val tvPlayer2Heroes = dialogView.findViewById<TextView>(R.id.tvPlayer2Heroes)
            tvPlayer1Heroes.text = yourRoles
            tvPlayer2Heroes.text = otherRoles
            
            // Create and show the dialog
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .create()
                
            // Style button on dialog show - use the same teal_200 color as headers
            dialog.setOnShowListener {
                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setTextColor(ContextCompat.getColor(this, R.color.teal_200))
            }
            
            dialog.show()
            
            // Save this comparison for future reference
            val otherSummaryObj = otherData.optJSONObject("profile")?.optJSONObject("summary")
            val otherBattletag = otherSummaryObj?.optString("id") ?: otherPlayerName
            saveLastComparedPlayer(otherBattletag, otherPlayerName, otherData)
            
        } catch (e: Exception) {
            // Security: Error handling - logging removed
            Toast.makeText(this, "Error creating comparison: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Compare win/loss stats between players
     * Updated to match Dota's comparison style
     */
    private fun compareWinLossStats(container: LinearLayout, yourData: JSONObject, otherData: JSONObject) {
        try {
            // Extract stats
            val yourProfile = yourData.optJSONObject("profile")
            val otherProfile = otherData.optJSONObject("profile")
            
            // Get the statistics
            val yourStats = yourProfile?.optJSONObject("stats")
            val otherStats = otherProfile?.optJSONObject("stats")
            
            if (yourStats != null && otherStats != null) {
                // Games won
                val yourGamesWon = yourStats.optInt("games_won", 0)
                val otherGamesWon = otherStats.optInt("games_won", 0)
                addComparisonRow(container, "Games Won", yourGamesWon.toString(), otherGamesWon.toString(), yourGamesWon > otherGamesWon)
                
                // Win rate (calculate if possible)
                val yourGamesPlayed = yourStats.optInt("games_played", 0)
                val otherGamesPlayed = otherStats.optInt("games_played", 0)
                
                val yourWinRate = if (yourGamesPlayed > 0) (yourGamesWon.toFloat() / yourGamesPlayed * 100) else 0f
                val otherWinRate = if (otherGamesPlayed > 0) (otherGamesWon.toFloat() / otherGamesPlayed * 100) else 0f
                
                addComparisonRow(
                    container, 
                    "Win Rate", 
                    String.format("%.1f%%", yourWinRate), 
                    String.format("%.1f%%", otherWinRate), 
                    yourWinRate > otherWinRate
                )
                
                // Games played
                addComparisonRow(
                    container,
                    "Games Played",
                    yourGamesPlayed.toString(),
                    otherGamesPlayed.toString(),
                    yourGamesPlayed > otherGamesPlayed
                )
            }
        } catch (e: Exception) {
            // Security: Error handling - logging removed
        }
    }
    
    /**
     * Compare playtime between players
     * Updated to match Dota's comparison style
     */
    private fun comparePlaytime(container: LinearLayout, yourData: JSONObject, otherData: JSONObject) {
        try {
            // Extract stats
            val yourProfile = yourData.optJSONObject("profile")
            val otherProfile = otherData.optJSONObject("profile")
            
            // Get the statistics
            val yourStats = yourProfile?.optJSONObject("stats")
            val otherStats = otherProfile?.optJSONObject("stats")
            
            if (yourStats != null && otherStats != null) {
                // Total playtime
                val yourPlaytime = yourStats.optInt("time_played", 0)
                val otherPlaytime = otherStats.optInt("time_played", 0)
                
                // Format playtime
                val yourHours = yourPlaytime / 3600
                val yourMins = (yourPlaytime % 3600) / 60
                val otherHours = otherPlaytime / 3600
                val otherMins = (otherPlaytime % 3600) / 60
                
                addComparisonRow(
                    container, 
                    "Total Playtime", 
                    "${yourHours}h ${yourMins}m", 
                    "${otherHours}h ${otherMins}m", 
                    yourPlaytime > otherPlaytime
                )
                
                // Average playtime per game if we have games played
                val yourGamesPlayed = yourStats.optInt("games_played", 0)
                val otherGamesPlayed = otherStats.optInt("games_played", 0)
                
                if (yourGamesPlayed > 0 && otherGamesPlayed > 0) {
                    val yourAvgSeconds = yourPlaytime / yourGamesPlayed
                    val otherAvgSeconds = otherPlaytime / otherGamesPlayed
                    
                    val yourAvgMins = yourAvgSeconds / 60
                    val yourAvgSecs = yourAvgSeconds % 60
                    val otherAvgMins = otherAvgSeconds / 60
                    val otherAvgSecs = otherAvgSeconds % 60
                    
                    addComparisonRow(
                        container,
                        "Avg. Game Time",
                        "${yourAvgMins}m ${yourAvgSecs}s",
                        "${otherAvgMins}m ${otherAvgSecs}s",
                        // For average game time, neither is necessarily better
                        yourAvgSeconds > otherAvgSeconds
                    )
                }
            }
        } catch (e: Exception) {
            // Security: Error handling - logging removed
        }
    }
    
    /**
     * Compare general player stats
     * Updated to match Dota's comparison style
     */
    private fun compareGeneralStats(container: LinearLayout, yourData: JSONObject, otherData: JSONObject) {
        try {
            // Extract stats
            val yourProfile = yourData.optJSONObject("profile")
            val otherProfile = otherData.optJSONObject("profile")
            
            // Compare summary information
            val yourSummary = yourProfile?.optJSONObject("summary")
            val otherSummary = otherProfile?.optJSONObject("summary")
            
            if (yourSummary != null && otherSummary != null) {
                // Player level
                val yourLevel = yourSummary.optInt("level", 0)
                val otherLevel = otherSummary.optInt("level", 0)
                addComparisonRow(container, "Player Level", yourLevel.toString(), otherLevel.toString(), yourLevel > otherLevel)
                
                // Endorsement level
                val yourEndorsement = yourSummary.optJSONObject("endorsement")?.optInt("level", 0) ?: 0
                val otherEndorsement = otherSummary.optJSONObject("endorsement")?.optInt("level", 0) ?: 0
                addComparisonRow(container, "Endorsement", yourEndorsement.toString(), otherEndorsement.toString(), yourEndorsement > otherEndorsement)
            }
            
            // Compare stats
            val yourStats = yourProfile?.optJSONObject("stats")
            val otherStats = otherProfile?.optJSONObject("stats")
            
            if (yourStats != null && otherStats != null) {
                // Eliminations
                val yourElims = yourStats.optInt("eliminations", 0)
                val otherElims = otherStats.optInt("eliminations", 0)
                addComparisonRow(container, "Eliminations", yourElims.toString(), otherElims.toString(), yourElims > otherElims)
                
                // Deaths
                val yourDeaths = yourStats.optInt("deaths", 0)
                val otherDeaths = otherStats.optInt("deaths", 0)
                addComparisonRow(container, "Deaths", yourDeaths.toString(), otherDeaths.toString(), yourDeaths < otherDeaths) // Lower deaths is better
                
                // Compare K/D ratio if both players have deaths
                if (yourDeaths > 0 && otherDeaths > 0) {
                    val yourKD = yourElims.toFloat() / yourDeaths
                    val otherKD = otherElims.toFloat() / otherDeaths
                    addComparisonRow(
                        container,
                        "K/D Ratio", 
                        String.format("%.2f", yourKD),
                        String.format("%.2f", otherKD),
                        yourKD > otherKD
                    )
                }
                
                // Healing
                val yourHealing = yourStats.optInt("healing", 0)
                val otherHealing = otherStats.optInt("healing", 0)
                if (yourHealing > 0 || otherHealing > 0) {
                    addComparisonRow(container, "Healing Done", yourHealing.toString(), otherHealing.toString(), yourHealing > otherHealing)
                }
                
                // Damage
                val yourDamage = yourStats.optInt("damage", 0)
                val otherDamage = otherStats.optInt("damage", 0)
                if (yourDamage > 0 || otherDamage > 0) {
                    addComparisonRow(container, "Damage Done", yourDamage.toString(), otherDamage.toString(), yourDamage > otherDamage)
                }
                
                // Final blows
                val yourFinalBlows = yourStats.optInt("final_blows", 0)
                val otherFinalBlows = otherStats.optInt("final_blows", 0)
                if (yourFinalBlows > 0 || otherFinalBlows > 0) {
                    addComparisonRow(container, "Final Blows", yourFinalBlows.toString(), otherFinalBlows.toString(), yourFinalBlows > otherFinalBlows)
                }
                
                // Solo kills
                val yourSoloKills = yourStats.optInt("solo_kills", 0)
                val otherSoloKills = otherStats.optInt("solo_kills", 0)
                if (yourSoloKills > 0 || otherSoloKills > 0) {
                    addComparisonRow(container, "Solo Kills", yourSoloKills.toString(), otherSoloKills.toString(), yourSoloKills > otherSoloKills)
                }
            }
        } catch (e: Exception) {
            // Security: Error handling - logging removed
        }
    }

    /**
     * Add a comparison row with stat highlighting
     */
    private fun addComparisonRow(container: LinearLayout, statName: String, yourValue: String, otherValue: String, yourIsBetter: Boolean) {
        val rowLayout = LinearLayout(this)
        rowLayout.orientation = LinearLayout.HORIZONTAL
        rowLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        rowLayout.setPadding(0, 8, 0, 8)
        
        // Stat name (20%)
        val nameView = TextView(this)
        nameView.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            0.2f
        )
        nameView.text = statName
        nameView.setTypeface(null, android.graphics.Typeface.BOLD)
        rowLayout.addView(nameView)
        
        // Your value (40%)
        val yourValueView = TextView(this)
        yourValueView.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            0.4f
        )
        yourValueView.text = yourValue
        if (yourIsBetter) {
            yourValueView.setTextColor(resources.getColor(android.R.color.holo_green_dark, theme))
            yourValueView.setTypeface(null, android.graphics.Typeface.BOLD)
        }
        rowLayout.addView(yourValueView)
        
        // Other value (40%)
        val otherValueView = TextView(this)
        otherValueView.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            0.4f
        )
        otherValueView.text = otherValue
        if (!yourIsBetter) {
            otherValueView.setTextColor(resources.getColor(android.R.color.holo_green_dark, theme))
            otherValueView.setTypeface(null, android.graphics.Typeface.BOLD)
        }
        rowLayout.addView(otherValueView)
        
        container.addView(rowLayout)
        
        // Add divider
        val divider = View(this)
        divider.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            1
        )
        divider.setBackgroundColor(resources.getColor(android.R.color.darker_gray, theme))
        container.addView(divider)
    }
    
    /**
     * Add a divider line to a container
     */
    private fun addDivider(container: LinearLayout) {
        val divider = View(this)
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
        params.setMargins(0, 16, 0, 16)
        divider.layoutParams = params
        divider.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        container.addView(divider)
    }
    
    /**
     * Extract top roles/heroes from player data for the comparison dialog
     * Similar to the extractTopHeroesInfo method in DotaStatsActivity
     */
    private fun extractTopRoles(playerData: JSONObject): String {
        try {
            val heroes = playerData.optJSONObject("heroes")
            if (heroes == null) {
                return "No hero data available"
            }
            
            val result = StringBuilder()
            
            // Try to get top 3 heroes by playtime
            val topHeroes = ArrayList<Pair<String, Int>>()
            val iterator = heroes.keys()
            
            while (iterator.hasNext()) {
                val heroName = iterator.next()
                val heroData = heroes.optJSONObject(heroName)
                val playtime = heroData?.optInt("time_played", 0) ?: 0
                
                if (playtime > 0) {
                    topHeroes.add(Pair(heroName, playtime))
                }
            }
            
            // Sort by playtime (descending)
            topHeroes.sortByDescending { it.second }
            
            // Format the top 3 (or fewer if not available)
            val count = minOf(3, topHeroes.size)
            for (i in 0 until count) {
                val (name, time) = topHeroes[i]
                val hours = time / 3600
                val minutes = (time % 3600) / 60
                
                result.append(name)
                result.append(" (")
                if (hours > 0) {
                    result.append(hours).append("h ")
                }
                result.append(minutes).append("m)")
                
                if (i < count - 1) {
                    result.append("\n")
                }
            }
            
            return if (result.isNotEmpty()) result.toString() else "No hero data"
            
        } catch (e: Exception) {
            // Security: Error handling - logging removed
            return "Error getting hero data"
        }
    }
    
    /**
     * Highlights the better stat between two players
     * Identical to the method in DotaStatsActivity for consistency
     */
    private fun highlightBetterStat(stat1: TextView, stat2: TextView, higherIsBetter: Boolean) {
        try {
            val value1 = stat1.text.toString().replace("%", "").toFloat()
            val value2 = stat2.text.toString().replace("%", "").toFloat()
            
            when {
                value1 > value2 && higherIsBetter -> {
                    stat1.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                    stat2.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                }
                value2 > value1 && higherIsBetter -> {
                    stat2.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                    stat1.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                }
                value1 > value2 && !higherIsBetter -> {
                    stat2.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                    stat1.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                }
                value2 > value1 && !higherIsBetter -> {
                    stat1.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                    stat2.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                }
                else -> {
                    // Equal values, both neutral
                    stat1.setTextColor(ContextCompat.getColor(this, android.R.color.tab_indicator_text))
                    stat2.setTextColor(ContextCompat.getColor(this, android.R.color.tab_indicator_text))
                }
            }
        } catch (e: Exception) {
            // Silent catch if parsing fails
            // Security: Error handling - logging removed
        }
    }
    
    /**
     * Update the friends list with recently compared players
     * This method is now deprecated as we use updateLastComparedPlayerUI instead
     * Kept for compatibility with any existing code that might call it
     */
    private fun updateFriendsListWithLastCompared() {
        // Just delegate to the new implementation
        updateLastComparedPlayerUI()
    }
    
    /**
     * Compare current player with a pro player by battletag
     * This is now a wrapper around the unified compareWithPlayer method
     * for backward compatibility
     */
    private fun compareWithProPlayer(proBattletag: String) {
        // Special case for WarDevil
        val displayName = if (proBattletag == "WarDevil-11626") {
            "WarDevil (Pro)"
        } else {
            null
        }
        
        // Delegate to our unified method
        compareWithPlayer(proBattletag, displayName)
    }
    
    private fun showRemoveConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Remove Overwatch")
            .setMessage("Are you sure you want to remove Overwatch from your profile?")
            .setPositiveButton("Yes") { _, _ ->
                // TODO: Implement removal logic
                Toast.makeText(this, "Overwatch removed from your profile", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }
    
    /**
     * Helper method to extract keys from a JSON object for debugging purposes
     */
    private fun getKeysFromJSON(jsonObj: JSONObject?): String {
        if (jsonObj == null) return "null"
        
        val keys = mutableListOf<String>()
        val iterator = jsonObj.keys()
        while (iterator.hasNext()) {
            keys.add(iterator.next())
        }
        
        return keys.joinToString(", ")
    }
    
    /**
     * Extract top roles/heroes from a JSON array
     */
    private fun extractTopRoles(roles: JSONArray?): String {
        if (roles == null || roles.length() == 0) {
            return "No hero data"
        }
        
        val sb = StringBuilder()
        val maxRoles = minOf(3, roles.length())
        
        for (i in 0 until maxRoles) {
            val role = roles.optJSONObject(i)
            val roleName = role?.optString("name", "Unknown")
            val playtime = role?.optInt("time_played", 0) ?: 0
            
            sb.append(roleName)
                .append(": ")
                .append(playtime)
                .append(" hrs")
            
            if (i < maxRoles - 1) {
                sb.append("\n")
            }
        }
        
        return sb.toString()
    }
}
