package com.example.minor_secure_programming

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.minor_secure_programming.api.ApiService
import com.example.minor_secure_programming.utils.SupabaseManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.json.JSONObject
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
    private var lastComparedBattletag: String? = null
    private var lastComparedPlayerName: String? = null
    private var lastComparedPlayerData: JSONObject? = null
    
    // UI Components
    private lateinit var etBattletag: TextInputEditText
    private lateinit var btnSearch: Button
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
    private lateinit var btnSearchFriend: Button
    private lateinit var containerFriends: LinearLayout
    private lateinit var tvNoFriends: TextView
    
    // Pro player list for comparison
    private val proPlayers = listOf(
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
            searchPlayer(formattedUsername)
        }
    }
    
    private fun initializeUI() {
        // Buttons
        btnRemove = findViewById(R.id.btn_remove_overwatch)
        btnSearch = findViewById(R.id.btn_search_overwatch)
        btnCompare = findViewById(R.id.btn_compare)
        
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
        
        // Input field for battletag
        etBattletag = findViewById(R.id.et_overwatch_battletag)
        
        // Cards for displaying sections
        cardPlayerStats = findViewById(R.id.card_player_stats)
        cardComparison = findViewById(R.id.card_comparison)
        cardFriends = findViewById(R.id.card_friends)
        
        // Pro player selection spinner
        spinnerProPlayers = findViewById(R.id.spinner_pro_players)
        
        // Layout for comparison results
        layoutComparisonResults = findViewById(R.id.layout_comparison_results)
        
        // Friends search and comparison
        etFriendBattletag = findViewById(R.id.et_friend_battletag)
        btnSearchFriend = findViewById(R.id.btn_search_friend)
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
                Log.d("OverwatchStats", "Bottom navigation view not found in layout")
            }
        } catch (e: Exception) {
            Log.e("OverwatchStats", "Error setting up navigation: ${e.message}")
        }
    }
    
    /**
     * Set up all button click listeners
     */
    private fun setupButtonClickListeners() {
        // Search button
        btnSearch.setOnClickListener {
            val battletag = etBattletag.text.toString().trim()
            if (battletag.isNotEmpty()) {
                // Format for API: replace # with - if present
                val formattedBattletag = battletag.replace("#", "-")
                searchPlayer(formattedBattletag)
            } else {
                Toast.makeText(this, "Please enter a battletag", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Compare with pro player button
        btnCompare.setOnClickListener {
            val selectedProPlayer = proPlayers[spinnerProPlayers.selectedItemPosition]
            // Format for API: replace # with - if present
            val formattedBattletag = selectedProPlayer.replace("#", "-")
            compareWithProPlayer(formattedBattletag)
        }
        
        // Friend search button
        btnSearchFriend.setOnClickListener {
            val battletag = etFriendBattletag.text.toString().trim()
            if (battletag.isNotEmpty()) {
                val formattedBattletag = battletag.replace("#", "-")
                searchPlayerForComparison(formattedBattletag)
            } else {
                Toast.makeText(this, "Please enter a friend's battletag", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Load last compared player from SharedPreferences
     */
    private fun loadLastComparedPlayer() {
        lastComparedBattletag = sharedPreferences.getString(KEY_LAST_COMPARED_BATTLETAG, null)
        lastComparedPlayerName = sharedPreferences.getString(KEY_LAST_COMPARED_NAME, null)
        
        if (lastComparedBattletag != null && lastComparedPlayerName != null) {
            Log.d("OverwatchStats", "Loading last compared player: $lastComparedBattletag, $lastComparedPlayerName")
            updateLastComparedPlayerUI()
        } else {
            Log.d("OverwatchStats", "No last compared player found")
        }
    }
    
    /**
     * Save last compared player to SharedPreferences
     */
    private fun saveLastComparedPlayer(battletag: String, playerName: String, playerData: JSONObject?) {
        Log.d("OverwatchStats", "Saving last compared player: $battletag, $playerName")
        
        lastComparedBattletag = battletag
        lastComparedPlayerName = playerName
        lastComparedPlayerData = playerData
        
        val editor = sharedPreferences.edit()
        editor.putString(KEY_LAST_COMPARED_BATTLETAG, battletag)
        editor.putString(KEY_LAST_COMPARED_NAME, playerName)
        editor.apply()
        
        // Update UI to show the last compared player
        updateLastComparedPlayerUI()
    }
    
    /**
     * Update the UI to show the last compared player in the friends section
     */
    private fun updateLastComparedPlayerUI() {
        if (lastComparedBattletag == null || lastComparedPlayerName == null) {
            return
        }
        
        // Remove "no friends" text if present
        tvNoFriends.visibility = View.GONE
        
        // Check if we already have a "Last compared" entry
        var lastComparedView: View? = null
        for (i in 0 until containerFriends.childCount) {
            val child = containerFriends.getChildAt(i)
            val titleView = child.findViewById<TextView>(R.id.tv_friend_title)
            if (titleView != null && titleView.text == "Last compared") {
                lastComparedView = child
                break
            }
        }
        
        if (lastComparedView == null) {
            // Create new view if it doesn't exist
            val inflater = LayoutInflater.from(this)
            lastComparedView = inflater.inflate(R.layout.item_overwatch_friend, containerFriends, false)
            containerFriends.addView(lastComparedView)
        }
        
        // Update the friend entry
        val tvFriendTitle = lastComparedView?.findViewById<TextView>(R.id.tv_friend_title)
        val tvFriendBattletag = lastComparedView?.findViewById<TextView>(R.id.tv_friend_battletag)
        val btnFriendCompare = lastComparedView?.findViewById<Button>(R.id.btn_friend_compare)
        
        tvFriendTitle?.text = "Last compared"
        tvFriendBattletag?.text = lastComparedPlayerName ?: "Unknown Player"
        
        // Set up compare button click listener
        btnFriendCompare?.setOnClickListener {
            if (lastComparedBattletag != null && currentPlayerData != null) {
                if (lastComparedPlayerData != null) {
                    // If we already have the data, show comparison directly
                    showComparisonDialog(currentPlayerData!!, lastComparedPlayerData!!, lastComparedPlayerName ?: "Unknown")
                } else {
                    // Otherwise reload the data
                    loadAndCompareWithPlayer(lastComparedBattletag!!)
                }
            } else {
                Toast.makeText(this, "Cannot compare: Missing player data", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Search for a player for comparison without overriding the main stats
     */
    private fun searchPlayerForComparison(battletag: String) {
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
                            Log.d("OverwatchStats", "Player comparison data: ${data.toString().substring(0, Math.min(500, data.toString().length))}")
                            
                            // Extract player name from profile
                            val profile = data.optJSONObject("profile")
                            val summary = profile?.optJSONObject("summary")
                            val playerName = summary?.optString("username", "Unknown") ?: "Unknown"
                            
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
                Log.e("OverwatchStats", "Exception during API call", e)
                showError("Error: ${e.message}")
            }
        }
    }
    
    /**
     * Load and compare with player by battletag
     */
    private fun loadAndCompareWithPlayer(battletag: String) {
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
                            // Extract player name
                            val profile = data.optJSONObject("profile")
                            val summary = profile?.optJSONObject("summary")
                            val playerName = summary?.optString("username", battletag) ?: battletag
                            
                            // Save this data for future comparisons
                            lastComparedPlayerData = data
                            
                            // Show comparison
                            if (currentPlayerData != null) {
                                showComparisonDialog(currentPlayerData!!, data, playerName)
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
                Log.e("OverwatchStats", "Exception during API call", e)
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
        Log.e("OverwatchStats", "Error: $message")
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
                    Log.d("OverwatchStats", "API Response: ${response.toString().substring(0, Math.min(500, response.toString().length))}")
                    
                    if (response != null && response.optBoolean("success", false)) {
                        val data = response.optJSONObject("data")
                        if (data != null) {
                            Log.d("OverwatchStats", "Player data: ${data.toString().substring(0, Math.min(500, data.toString().length))}")
                            
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
                                            Log.e("OverwatchStats", "Failed to save stats: ${error?.message}", error)
                                            Snackbar.make(
                                                findViewById(android.R.id.content),
                                                "Profile displayed but couldn't save stats: ${error?.message ?: "Unknown error"}",
                                                Snackbar.LENGTH_SHORT
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        savingSnackbar.dismiss()
                                        Log.e("OverwatchStats", "Exception saving stats", e)
                                        Snackbar.make(
                                            findViewById(android.R.id.content),
                                            "Error saving stats: ${e.message ?: "Unknown error"}",
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } ?: run {
                                // No game ID available
                                Log.w("OverwatchStats", "No game ID available to save stats")
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
                Log.e("OverwatchStats", "Exception during API call", e)
                showError("Error: ${e.message}")
            }
        }
    }
    
    private fun displayPlayerData(data: JSONObject) {
        try {
            Log.d("OverwatchStats", "Displaying player data: ${data.toString().substring(0, Math.min(500, data.toString().length))}")
            
            // Extract profile data
            val profile = data.optJSONObject("profile")
            Log.d("OverwatchStats", "Profile object: ${profile?.toString() ?: "null"}")
            
            if (profile != null) {
                // The player data is in a nested 'summary' object
                val summary = profile.optJSONObject("summary")
                Log.d("OverwatchStats", "Summary object: ${summary?.toString() ?: "null"}")
                
                if (summary != null) {
                    val playerName = summary.optString("username", "Unknown")
                    
                    // Player level might not be directly available in the API response
                    // Fall back to calculating level from stats if needed
                    val playerLevel = summary.optInt("player_level", 0)
                    
                    // Endorsement is nested in an object
                    val endorsementObj = summary.optJSONObject("endorsement")
                    val endorsementLevel = endorsementObj?.optInt("level", 0) ?: 0
                    
                    Log.d("OverwatchStats", "Player name: $playerName, Level: $playerLevel, Endorsement: $endorsementLevel")
                    
                    // Update UI
                    tvPlayerName.text = playerName
                    tvPlayerLevel.text = "Level: $playerLevel"
                    tvEndorsement.text = "Endorsement: $endorsementLevel"
                    
                    // TODO: Load avatar image using a library like Glide or Picasso
                    val avatarUrl = summary.optString("avatar", "")
                    Log.d("OverwatchStats", "Avatar URL: $avatarUrl")
                    // Glide.with(this).load(avatarUrl).into(imgPlayerAvatar)
                    
                    // No longer displaying competitive rankings
                    
                    // Handle stats
                    displayPlayerStats(profile)
                } else {
                    Log.w("OverwatchStats", "Summary object is null in profile")
                    setDefaultPlayerInfo()
                    setDefaultRanks()
                    setDefaultStats()
                }
            } else {
                Log.w("OverwatchStats", "Profile object is null in response")
                setDefaultPlayerInfo()
                setDefaultRanks()
                setDefaultStats()
            }
        } catch (e: Exception) {
            Log.e("OverwatchStats", "Error displaying player data", e)
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
    Log.d("OverwatchStats", "Skipping competitive rank display as requested")
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
                            
                            Log.d("OverwatchStats", "Stats loaded - Games: $gamesPlayed, Wins: $gamesWon, Eliminations: $eliminations")
                        } else {
                            Log.w("OverwatchStats", "All-heroes array is null")
                            setDefaultStats()
                        }
                    } else {
                        Log.w("OverwatchStats", "Career stats object is null")
                        setDefaultStats()
                    }
                } else {
                    Log.w("OverwatchStats", "Quickplay object is null")
                    setDefaultStats()
                }
            } else {
                Log.w("OverwatchStats", "PC object is null")
                setDefaultStats()
            }
        } else {
            Log.w("OverwatchStats", "Stats object is null")
            setDefaultStats()
        }
    } catch (e: Exception) {
        Log.e("OverwatchStats", "Error processing stats", e)
        setDefaultStats()
    }
    
    // Show player stats card
    cardPlayerStats.visibility = View.VISIBLE
}

/**
 * Custom comparison dialog that shows player stats side-by-side with highlighting
 */
private fun showComparisonDialog(yourData: JSONObject, otherData: JSONObject, otherPlayerName: String) {
    try {
        // Extract player names
        val yourProfile = yourData.optJSONObject("profile")
        val yourSummary = yourProfile?.optJSONObject("summary")
        val yourName = yourSummary?.optString("username", "You") ?: "You"
        
        // Create dialog builder
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Player Comparison")
        
        // Create dialog content view
        val contentView = layoutInflater.inflate(R.layout.dialog_comparison, null)
        val container = contentView.findViewById<LinearLayout>(R.id.comparison_container)
        
        // Add header row with player names
        val headerLayout = LinearLayout(this)
        headerLayout.orientation = LinearLayout.HORIZONTAL
        headerLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        
        // Stats label (20%)
        val labelView = TextView(this)
        labelView.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            0.2f
        )
        labelView.text = "Stats"
        labelView.textSize = 16f
        labelView.setTypeface(null, android.graphics.Typeface.BOLD)
        headerLayout.addView(labelView)
        
        // Your name (40%)
        val yourNameView = TextView(this)
        yourNameView.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            0.4f
        )
        yourNameView.text = yourName
        yourNameView.textSize = 16f
        yourNameView.setTypeface(null, android.graphics.Typeface.BOLD)
        headerLayout.addView(yourNameView)
        
        // Other player name (40%)
        val otherNameView = TextView(this)
        otherNameView.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            0.4f
        )
        otherNameView.text = otherPlayerName
        otherNameView.textSize = 16f
        otherNameView.setTypeface(null, android.graphics.Typeface.BOLD)
        headerLayout.addView(otherNameView)
        
        container.addView(headerLayout)
        
        // Add divider
        val divider = View(this)
        divider.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            2
        )
        divider.setBackgroundColor(resources.getColor(android.R.color.darker_gray, theme))
        container.addView(divider)
        
        // Compare win/loss stats
        compareWinLossStats(container, yourData, otherData)
        
        // Compare playtime
        comparePlaytime(container, yourData, otherData)
        
        // Compare other stats if available
        compareGeneralStats(container, yourData, otherData)
        
        builder.setView(contentView)
        builder.setPositiveButton("Close", null)
        builder.create().show()
        
    } catch (e: Exception) {
        Log.e("OverwatchStats", "Error showing comparison dialog", e)
        Toast.makeText(this, "Error creating comparison: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
    
    /**
     * Compare win/loss stats between players
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
            }
        } catch (e: Exception) {
            Log.e("OverwatchStats", "Error comparing win/loss stats", e)
        }
    }
    
    /**
     * Compare playtime between players
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
                // Time played
                val yourTime = yourStats.optInt("time_played_seconds", 0)
                val otherTime = otherStats.optInt("time_played_seconds", 0)
                
                // Convert to hours and minutes
                val yourHours = yourTime / 3600
                val yourMinutes = (yourTime % 3600) / 60
                val otherHours = otherTime / 3600
                val otherMinutes = (otherTime % 3600) / 60
                
                addComparisonRow(
                    container, 
                    "Time Played", 
                    "${yourHours}h ${yourMinutes}m", 
                    "${otherHours}h ${otherMinutes}m", 
                    yourTime > otherTime
                )
            }
        } catch (e: Exception) {
            Log.e("OverwatchStats", "Error comparing playtime", e)
        }
    }
    
    /**
     * Compare general player stats
     */
    private fun compareGeneralStats(container: LinearLayout, yourData: JSONObject, otherData: JSONObject) {
        try {
            // Extract player information
            val yourProfile = yourData.optJSONObject("profile")
            val otherProfile = otherData.optJSONObject("profile")
            
            // Compare player levels
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
            }
        } catch (e: Exception) {
            Log.e("OverwatchStats", "Error comparing general stats", e)
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
     * Compare current player with a pro player by battletag
     */
    private fun compareWithProPlayer(proBattletag: String) {
        if (currentPlayerData == null) {
            Toast.makeText(this, "You need to load your profile first", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setView(layoutInflater.inflate(R.layout.dialog_loading_overwatch, null))
            .setCancelable(false)
            .create()
            
        loadingDialog.show()
        
        lifecycleScope.launch {
            try {
                val result = apiService.getOverwatchCombinedProfile(proBattletag)
                
                // Dismiss loading dialog
                loadingDialog.dismiss()
                
                if (result.isSuccess) {
                    val response = result.getOrNull()
                    
                    if (response != null && response.optBoolean("success", false)) {
                        val proData = response.optJSONObject("data")
                        if (proData != null) {
                            // Extract player name
                            val proProfile = proData.optJSONObject("profile")
                            val proSummary = proProfile?.optJSONObject("summary")
                            val proPlayerName = proSummary?.optString("username", proBattletag) ?: proBattletag
                            
                            // Show the comparison dialog
                            showComparisonDialog(currentPlayerData!!, proData, proPlayerName)
                        } else {
                            showError("Invalid pro player data response")
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
                Log.e("OverwatchStats", "Error loading pro data", e)
                showError("Error: ${e.message}")
            }
        }
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
    
}
