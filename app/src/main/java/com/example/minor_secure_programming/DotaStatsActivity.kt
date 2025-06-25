package com.example.minor_secure_programming

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
// Use explicit import for Button to avoid ambiguity
import android.widget.Button as AndroidButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.minor_secure_programming.api.ApiService
import com.example.minor_secure_programming.utils.SupabaseManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONArray

class DotaStatsActivity : AppCompatActivity() {
    
    private lateinit var apiService: ApiService
    private val TAG = "DotaStatsActivity"
    private var gameId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dota_stats)
        
        // Initialize API service
        apiService = ApiService(this)
        
        // Setup action bar with back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupNavigationListeners()
        setupButtonClickListeners()
        
        // Load last compared player from SharedPreferences
        loadLastComparedPlayer()
        
        // Setup friend buttons with a short delay to ensure views are inflated
        Handler(Looper.getMainLooper()).postDelayed({
            setupFriendCompareButtons()
        }, 200)
        
        // Get stored game data from intent
        val username = intent.getStringExtra("USERNAME") ?: ""
        gameId = intent.getStringExtra("GAME_ID")
        val steamId = getStoredSteamId(username)
        
        // Set up remove game button
        findViewById<AndroidButton>(R.id.btnRemoveGame).setOnClickListener {
            val gameName = "DOTA 2"
            showRemoveGameConfirmation(gameName, username)
        }
        
        // Friend comparison buttons are set up through setupFriendCompareButtons()
        
        // Navigation is handled by setupNavigationListeners()
        
        // Fetch and display player profile based on Steam ID if available, otherwise use username
        if (!steamId.isNullOrEmpty()) {
            fetchAndDisplayPlayerProfile(steamId)
        } else {
            fetchAndDisplayPlayerProfile(username)
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun showRemoveGameConfirmation(gameName: String, username: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Remove Game")
        builder.setMessage("Are you sure you want to remove $gameName from your profile?")
        
        builder.setPositiveButton("Yes") { _, _ ->
            // In a real app, this would call an API to remove the game
            Toast.makeText(this, "$gameName removed from your profile", Toast.LENGTH_SHORT).show()
            finish() // Go back to previous screen
        }
        
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        
        val dialog = builder.create()
        dialog.show()
    }
    
    /**
     * Get the Steam ID associated with a username from SharedPreferences
     */
    fun getStoredSteamId(username: String): String? {
        val sharedPref = getSharedPreferences("games", Context.MODE_PRIVATE)
        val gameDataJson = sharedPref.getString("DOTA 2", null)
        
        if (gameDataJson != null) {
            try {
                val gameData = JSONObject(gameDataJson)
                val storedUsername = gameData.optString("username")
                
                // Only return Steam ID if this username matches the stored one
                if (username == storedUsername) {
                    return gameData.optString("steamId")
                }
            } catch (e: Exception) {
                // Security: Error handling - logging removed
            }
        }
        
        return null
    }
    
    /**
     * Set up navigation listeners
     */
    private fun setupNavigationListeners() {
        // Setup bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
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
    }
    
    /**
     * Set up all button click listeners
     */
    private fun setupButtonClickListeners() {
        // Set up search button for player comparison
        findViewById<AndroidButton>(R.id.btnSearch)?.setOnClickListener {
            val searchQuery = findViewById<EditText>(R.id.searchFriend)?.text?.toString()?.trim()
            if (!searchQuery.isNullOrEmpty()) {
                // Make API call but only show name and compare option
                searchPlayerForComparison(searchQuery)
            } else {
                Toast.makeText(this, "Please enter a username or Steam ID to search", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Set up compare stats button if it exists
        findViewById<AndroidButton>(R.id.btnCompareStats)?.setOnClickListener {
            // Use the last searched player if available, or a default example
            val comparisonId = lastSearchedPlayerId ?: "76561198811471235" // Example ID for testing
            showPlayerComparisonDialog(comparisonId)
        }
    }
    
    /**
     * Shows an error message to the user
     */
    fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    fun fetchAndDisplayPlayerProfile(playerIdOrName: String) {
        lifecycleScope.launch {
            try {
                // Show loading state
                val loadingSnackbar = Snackbar.make(
                    findViewById(android.R.id.content),
                    "Loading profile for $playerIdOrName...",
                    Snackbar.LENGTH_INDEFINITE
                ).apply { show() }
                
                // Get player profile data from API
                val result = apiService.getDotaPlayerProfile(playerIdOrName)
                
                if (result.isSuccess) {
                    val response = result.getOrNull()
                    val profileData = response?.optJSONObject("data")
                    
                    if (profileData != null) {
                        // Update UI with the fetched data
                        updateUIWithPlayerData(profileData)
                        loadingSnackbar.dismiss()
                        
                        // Save to Supabase if we have a game ID
                        gameId?.let { id ->
                            val statsResult = SupabaseManager.saveGameStats(id, profileData)
                            if (statsResult.isSuccess) {
                                Snackbar.make(
                                    findViewById(android.R.id.content),
                                    "Player stats saved successfully!",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                                
                                // Also fetch and save recent matches
                                fetchAndSaveRecentMatches(playerIdOrName, id)
                            } else {
                                val error = statsResult.exceptionOrNull()
                                // Security: Error handling - logging removed
                                Snackbar.make(
                                    findViewById(android.R.id.content),
                                    "Profile displayed but couldn't save stats",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        } ?: run {
                            // No game ID available
                            // Security: Warning removed
                        }
                    } else {
                        showError("Failed to parse player data")
                    }
                } else {
                    val error = result.exceptionOrNull()
                    showError("Error: ${error?.message ?: "Unknown error"}")
                    // Security: Error handling - logging removed
                }
                loadingSnackbar.dismiss()
            } catch (e: Exception) {
                showError("Error: ${e.message ?: "Unknown error"}")
                // Security: Error handling - logging removed
            }
        }
    }

    /**
     * Fetch player's recent matches and save them to the database
     */
    private fun fetchAndSaveRecentMatches(playerId: String, gameId: String) {
        lifecycleScope.launch {
            try {
                // Get player's recent matches (last 5)
                val result = apiService.getDotaPlayerMatches(playerId, 5)
                
                if (result.isSuccess) {
                    val matchesArray = result.getOrNull()
                    
                    if (matchesArray != null) {
                        // Create a combined JSON object with profile and matches data
                        val combinedStats = JSONObject().apply {
                            put("recent_matches", matchesArray)
                            put("last_updated", System.currentTimeMillis())
                        }
                        
                        // Save the matches data to Supabase
                        val saveResult = SupabaseManager.saveGameStats(gameId, combinedStats)
                        
                        if (saveResult.isSuccess) {
                            // Security: Logging removed
                        } else {
                            val error = saveResult.exceptionOrNull()
                            // Security: Error handling - logging removed
                        }
                    }
                } else {
                    val error = result.exceptionOrNull()
                    // Security: Error handling - logging removed
                }
            } catch (e: Exception) {
                // Security: Error handling - logging removed
            }
        }
    }

    private fun updateUIWithPlayerData(profileData: JSONObject) {
        try {
            // Extract player info
            val player = profileData.optJSONObject("player")
            val winLoss = profileData.optJSONObject("win_loss")
            val recentMatches = profileData.optJSONArray("recent_matches")
            val topHeroes = profileData.optJSONArray("top_heroes")
            
            if (player != null) {
                val profile = player.optJSONObject("profile")
                val personaname = profile?.optString("personaname", "Unknown Player")
                val avatarUrl = profile?.optString("avatarfull", "")
                val accountId = player.optString("account_id", "")
                
                // Update player name and ID
                val tvPlayerName = findViewById<TextView>(R.id.tvPlayerName)
                tvPlayerName.text = personaname
                
                val tvPlayerId = findViewById<TextView>(R.id.tvPlayerId)
                tvPlayerId.text = "ID: $accountId"
                
                // TODO: Load avatar image using an image loading library like Glide or Picasso
            }
            
            // Update win/loss stats
            if (winLoss != null) {
                val wins = winLoss.optInt("win", 0)
                val losses = winLoss.optInt("lose", 0)
                val total = wins + losses
                val winRate = if (total > 0) (wins.toFloat() / total.toFloat() * 100f) else 0f
                
                val tvWins = findViewById<TextView>(R.id.tvWins)
                tvWins.text = "Wins: $wins"
                
                val tvLosses = findViewById<TextView>(R.id.tvLosses)
                tvLosses.text = "Losses: $losses"
                
                val tvWinRate = findViewById<TextView>(R.id.tvWinRate)
                tvWinRate.text = "Win Rate: ${String.format("%.1f", winRate)}%"
            }
            
            // Update recent matches
            if (recentMatches != null && recentMatches.length() > 0) {
                val tvRecentMatches = findViewById<TextView>(R.id.tvRecentMatches)
                val matchesText = StringBuilder("Recent Matches:\n")
                
                for (i in 0 until minOf(5, recentMatches.length())) {
                    val match = recentMatches.getJSONObject(i)
                    val heroId = match.optInt("hero_id", 0)
                    val kills = match.optInt("kills", 0)
                    val deaths = match.optInt("deaths", 0)
                    val assists = match.optInt("assists", 0)
                    val isWin = match.optBoolean("radiant_win", false) == (match.optInt("player_slot", 0) < 128)
                    
                    matchesText.append("• ${if (isWin) "Win" else "Loss"} with Hero $heroId: $kills/$deaths/$assists\n")
                }
                
                tvRecentMatches.text = matchesText.toString()
            }
            
            // Update top heroes
            if (topHeroes != null && topHeroes.length() > 0) {
                val tvTopHeroes = findViewById<TextView>(R.id.tvTopHeroes)
                val heroesText = StringBuilder("Top Heroes:\n")
                
                for (i in 0 until minOf(5, topHeroes.length())) {
                    val hero = topHeroes.getJSONObject(i)
                    val heroId = hero.optInt("hero_id", 0)
                    val games = hero.optInt("games", 0)
                    val win = hero.optInt("win", 0)
                    val winRate = if (games > 0) (win.toFloat() / games.toFloat() * 100f) else 0f
                    
                    heroesText.append("• Hero $heroId: $win/$games (${String.format("%.1f", winRate)}%)\n")
                }
                
                tvTopHeroes.text = heroesText.toString()
            }
            
            Toast.makeText(this, "Profile loaded successfully", Toast.LENGTH_SHORT).show()
            
            // Store the profile data for comparison later
            this.lastLoadedProfile = profileData
            
        } catch (e: Exception) {
            // Security: Error handling - logging removed
            Toast.makeText(this, "Error parsing player data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Store the last loaded profile for comparison
    private var lastLoadedProfile: JSONObject? = null
    
    /**
     * Sets up click listeners for the friend comparison buttons
     */
    private fun setupFriendCompareButtons() {
        try {
            // Hardcode player names for the 3 pro players
            val proPlayer1 = "miracle"
            val proPlayer2 = "sumail"
            val proPlayer3 = "puppey"
            
            // Look for existing Compare buttons in the layout
            val friendCompareButtons = findCompareButtonsInLayout()
            
            // Assign click listeners based on how many buttons we found
            when (friendCompareButtons.size) {
                0 -> {
                    // No buttons found - log the issue
                    // Security: Logging removed
                }
                1 -> {
                    // Only found one button
                    friendCompareButtons[0].setOnClickListener {
                        showPlayerComparisonDialog(proPlayer1)
                    }
                }
                2 -> {
                    // Found two buttons
                    friendCompareButtons[0].setOnClickListener {
                        showPlayerComparisonDialog(proPlayer1)
                    }
                    friendCompareButtons[1].setOnClickListener {
                        showPlayerComparisonDialog(proPlayer2)
                    }
                }
                else -> {
                    // Found three or more buttons
                    friendCompareButtons[0].setOnClickListener {
                        showPlayerComparisonDialog(proPlayer1)
                    }
                    friendCompareButtons[1].setOnClickListener {
                        showPlayerComparisonDialog(proPlayer2)
                    }
                    friendCompareButtons[2].setOnClickListener {
                        showPlayerComparisonDialog(proPlayer3)
                    }
                }
            }
            
            // Also set up the top Compare button if it exists
            findViewById<AndroidButton?>(R.id.btnCompareStats)?.setOnClickListener {
                showPlayerComparisonDialog(proPlayer1) 
            }
            
        } catch (e: Exception) {
            // Security: Error handling - logging removed
        }
    }
    
    /**
     * Find all Compare buttons in the layout
     */
    private fun findCompareButtonsInLayout(): List<AndroidButton> {
        val result = ArrayList<AndroidButton>()
        // Search the entire view hierarchy for buttons
        val rootView = window.decorView.findViewById<View>(android.R.id.content)
        val viewGroup = rootView as ViewGroup
        
        // Add all buttons that say "Compare"
        searchForCompareButtons(viewGroup, result)
        
        return result
    }
    
    /**
     * Recursively search for "Compare" buttons in the view hierarchy
     */
    private fun searchForCompareButtons(viewGroup: ViewGroup?, result: ArrayList<AndroidButton>) {
        if (viewGroup == null) return
        
        val count = viewGroup.childCount
        for (i in 0 until count) {
            val view = viewGroup.getChildAt(i)
            
            if (view is AndroidButton && view.text?.toString() == "Compare") {
                result.add(view)
            } else if (view is ViewGroup) {
                searchForCompareButtons(view, result)
            }
        }
    }
    
    /**
     * Shows a side-by-side comparison dialog between current player and another player
     */
    private fun showPlayerComparisonDialog(otherPlayerName: String) {
        // Make sure we have data for the current player
        val currentProfile = lastLoadedProfile
        if (currentProfile == null) {
            Toast.makeText(this, "Current player profile not loaded yet", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show a loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setTitle("Loading Player Comparison")
            .setMessage("Getting data for $otherPlayerName...")
            .setCancelable(false)
            .show()
        
        // Fetch the other player's data
        lifecycleScope.launch {
            try {
                val result = apiService.getDotaPlayerProfile(otherPlayerName)
                
                // Dismiss the loading dialog
                loadingDialog.dismiss()
                
                if (result.isSuccess) {
                    val otherProfile = result.getOrNull()!!
                    
                    // Create a dialog layout for comparison
                    val inflater = LayoutInflater.from(this@DotaStatsActivity)
                    val dialogView = inflater.inflate(R.layout.dialog_compare_stats, null)
                    
                    // Populate the dialog with comparison data
                    populateComparisonDialog(dialogView, currentProfile, otherProfile)
                    
                    // Show the comparison dialog
                    AlertDialog.Builder(this@DotaStatsActivity)
                        .setView(dialogView)
                        .setPositiveButton("Close", null)
                        .show()
                    
                } else {
                    // Show error if player not found
                    showError("Unable to load data for $otherPlayerName. Error: ${result.exceptionOrNull()?.message ?: "Unknown error"}")
                }
                
            } catch (e: Exception) {
                loadingDialog.dismiss()
                showError("Error comparing with $otherPlayerName: ${e.message}")
                // Security: Error handling - logging removed
            }
        }
    }
    
    /**
     * Populates the comparison dialog with data from both players
     */
    private fun populateComparisonDialog(
        dialogView: View,
        currentPlayerData: JSONObject, 
        otherPlayerData: JSONObject
    ) {
        try {
            // Log the raw JSON data for debugging
            // Security: Logging removed
            // Security: Logging removed
            
            // Set player names - handle both API response formats
            // API might return either direct data or nested under 'data'
            val currentPlayerObj = currentPlayerData.optJSONObject("data") ?: currentPlayerData
            val otherPlayerObj = otherPlayerData.optJSONObject("data") ?: otherPlayerData
            
            val currentPlayer = currentPlayerObj.optJSONObject("player")
            val otherPlayer = otherPlayerObj.optJSONObject("player")
            
            val currentName = currentPlayer?.optJSONObject("profile")?.optString("personaname", "You") ?: "You"
            val otherName = otherPlayer?.optJSONObject("profile")?.optString("personaname", "Them") ?: "Them"
            
            dialogView.findViewById<TextView>(R.id.tvPlayer1Name).text = currentName
            dialogView.findViewById<TextView>(R.id.tvPlayer2Name).text = otherName
            
            // Set win/loss stats - handle possible nesting in 'data'
            val currentWL = currentPlayerObj.optJSONObject("win_loss")
            val otherWL = otherPlayerObj.optJSONObject("win_loss")
            
            // Wins
            val currentWins = currentWL?.optInt("win", 0) ?: 0
            val otherWins = otherWL?.optInt("win", 0) ?: 0
            dialogView.findViewById<TextView>(R.id.tvPlayer1Wins).text = currentWins.toString()
            dialogView.findViewById<TextView>(R.id.tvPlayer2Wins).text = otherWins.toString()
            
            // Colorize higher values
            highlightBetterStat(dialogView.findViewById(R.id.tvPlayer1Wins), dialogView.findViewById(R.id.tvPlayer2Wins), true)
            
            // Losses
            val currentLosses = currentWL?.optInt("lose", 0) ?: 0
            val otherLosses = otherWL?.optInt("lose", 0) ?: 0
            dialogView.findViewById<TextView>(R.id.tvPlayer1Losses).text = currentLosses.toString()
            dialogView.findViewById<TextView>(R.id.tvPlayer2Losses).text = otherLosses.toString()
            
            // Colorize lower values (for losses, lower is better)
            highlightBetterStat(dialogView.findViewById(R.id.tvPlayer1Losses), dialogView.findViewById(R.id.tvPlayer2Losses), false)
            
            // Win rates
            val currentTotal = currentWins + currentLosses
            val otherTotal = otherWins + otherLosses
            
            val currentWinRate = if (currentTotal > 0) (currentWins.toFloat() / currentTotal * 100f) else 0f
            val otherWinRate = if (otherTotal > 0) (otherWins.toFloat() / otherTotal * 100f) else 0f
            
            dialogView.findViewById<TextView>(R.id.tvPlayer1WinRate).text = String.format("%.1f%%", currentWinRate)
            dialogView.findViewById<TextView>(R.id.tvPlayer2WinRate).text = String.format("%.1f%%", otherWinRate)
            
            // Colorize higher values
            highlightBetterStat(dialogView.findViewById(R.id.tvPlayer1WinRate), dialogView.findViewById(R.id.tvPlayer2WinRate), true)
            
            // Format heroes info (using JSONArray from org.json)
            val currentHeroes = extractTopHeroesInfo(currentPlayerObj.optJSONArray("top_heroes"))
            val otherHeroes = extractTopHeroesInfo(otherPlayerObj.optJSONArray("top_heroes"))
            
            dialogView.findViewById<TextView>(R.id.tvPlayer1Heroes).text = currentHeroes
            dialogView.findViewById<TextView>(R.id.tvPlayer2Heroes).text = otherHeroes
            
        } catch (e: Exception) {
            // Security: Error handling - logging removed
        }
    }
    
    /**
     * Helper function to highlight the better stat between two players
     */
    private fun highlightBetterStat(stat1: TextView, stat2: TextView, higherIsBetter: Boolean) {
        try {
            val value1 = stat1.text.toString().replace("%", "").toFloat()
            val value2 = stat2.text.toString().replace("%", "").toFloat()
            
            when {
                value1 > value2 && higherIsBetter -> {
                    stat1.setTextColor(resources.getColor(android.R.color.holo_green_dark, theme))
                    stat2.setTextColor(resources.getColor(android.R.color.darker_gray, theme))
                }
                value2 > value1 && higherIsBetter -> {
                    stat2.setTextColor(resources.getColor(android.R.color.holo_green_dark, theme))
                    stat1.setTextColor(resources.getColor(android.R.color.darker_gray, theme))
                }
                value1 > value2 && !higherIsBetter -> {
                    stat2.setTextColor(resources.getColor(android.R.color.holo_green_dark, theme))
                    stat1.setTextColor(resources.getColor(android.R.color.darker_gray, theme))
                }
                value2 > value1 && !higherIsBetter -> {
                    stat1.setTextColor(resources.getColor(android.R.color.holo_green_dark, theme))
                    stat2.setTextColor(resources.getColor(android.R.color.darker_gray, theme))
                }
            }
        } catch (e: Exception) {
            // Silent catch if parsing fails
        }
    }
    
    /**
     * Search for a player without overriding the current player stats
     * Instead, show just their name and a compare button
     */
    private fun searchPlayerForComparison(playerIdOrName: String) {
        // Show a loading indicator
        val loadingDialog = AlertDialog.Builder(this)
            .setView(LayoutInflater.from(this).inflate(R.layout.dialog_loading, null))
            .setCancelable(false)
            .create()
        loadingDialog.show()
        
        // Make API call to search for player
        lifecycleScope.launch {
            try {
                val result = apiService.getDotaPlayerProfile(playerIdOrName)
                loadingDialog.dismiss()
                
                if (result.isSuccess) {
                    val profileData = result.getOrNull()!!
                    // Extract player data - handle both API response formats
                    val playerObj = profileData.optJSONObject("data") ?: profileData
                    val player = playerObj.optJSONObject("player")
                    
                    if (player != null) {
                        val playerName = player.optJSONObject("profile")?.optString("personaname", playerIdOrName) ?: playerIdOrName
                        val steamId = player.optString("account_id", playerIdOrName)
                        
                        // Save player info internally
                        saveLastSearchedPlayer(playerName, steamId, playerObj)
                        
                        // Show player found dialog with compare button
                        showPlayerFoundDialog(playerName, steamId)
                    } else {
                        Toast.makeText(this@DotaStatsActivity, "Player data not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Toast.makeText(this@DotaStatsActivity, "Error finding player: $error", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                loadingDialog.dismiss()
                Toast.makeText(this@DotaStatsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                // Security: Error handling - logging removed
            }
        }
    }
    
    /**
     * Save the last searched player in memory and SharedPreferences for persistence
     */
    private fun saveLastSearchedPlayer(name: String, steamId: String, playerData: JSONObject) {
        // Store in memory for current session
        lastSearchedPlayerName = name
        lastSearchedPlayerId = steamId
        lastSearchedPlayerData = playerData
        
        // Save to SharedPreferences for persistence between app launches
        val prefs = getSharedPreferences("dota_players", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("last_compared_name", name)
            .putString("last_compared_id", steamId)
            .putString("last_compared_data", playerData.toString())
            .apply()
        
        // Update UI to show in friends section
        updateLastComparedInUI(name)
    }
    
    // Variables to store last searched player
    private var lastSearchedPlayerName: String? = null
    private var lastSearchedPlayerId: String? = null
    private var lastSearchedPlayerData: JSONObject? = null
    
    /**
     * Load the last compared player from SharedPreferences and restore it in UI
     */
    private fun loadLastComparedPlayer() {
        try {
            val prefs = getSharedPreferences("dota_players", Context.MODE_PRIVATE)
            
            // Get saved player data
            val name = prefs.getString("last_compared_name", null)
            val id = prefs.getString("last_compared_id", null)
            val dataString = prefs.getString("last_compared_data", null)
            
            // If we have data, restore it
            if (name != null && id != null && dataString != null) {
                try {
                    // Parse the JSON string back to JSONObject
                    val playerData = JSONObject(dataString)
                    
                    // Store in memory variables
                    lastSearchedPlayerName = name
                    lastSearchedPlayerId = id
                    lastSearchedPlayerData = playerData
                    
                    // Update UI
                    updateLastComparedInUI(name)
                    
                    // Security: Logging removed
                } catch (e: Exception) {
                    // Security: Error handling - logging removed
                }
            } else {
                // Security: Logging removed
            }
        } catch (e: Exception) {
            // Security: Error handling - logging removed
        }
    }
    
    /**
     * Update the Friends section to show the last compared player 
     */
    private fun updateLastComparedInUI(name: String) {
        // Find friends container - using the main LinearLayout inside friendsCard
        val friendsCard = findViewById<androidx.cardview.widget.CardView>(R.id.friendsCard)
        // The main LinearLayout inside the friendsCard is the first child
        val friendsContainer = if (friendsCard != null) {
            (friendsCard.getChildAt(0) as? LinearLayout)
        } else null
        if (friendsContainer != null) {
            // Check if we already have a "Last compared" entry
            var lastComparedEntry: View? = null
            for (i in 0 until friendsContainer.childCount) {
                val child = friendsContainer.getChildAt(i)
                val friendLabel = child.findViewById<TextView>(R.id.tvFriendName)
                if (friendLabel != null && friendLabel.text == "Last compared") {
                    lastComparedEntry = child
                    break
                }
            }
            
            // If we have a "Last compared" entry, update it, otherwise create one
            if (lastComparedEntry != null) {
                val playerNameView = lastComparedEntry.findViewById<TextView>(R.id.tvPlayerName)
                playerNameView?.text = name
            } else {
                // Inflate a new friend entry view
                val inflater = LayoutInflater.from(this)
                val newFriendView = inflater.inflate(R.layout.item_friend, null)
                
                // Set it up with "Last compared" and player name
                newFriendView.findViewById<TextView>(R.id.tvFriendName).text = "Last compared"
                newFriendView.findViewById<TextView>(R.id.tvPlayerName).text = name
                
                // Set up compare button
                val compareButton = newFriendView.findViewById<AndroidButton>(R.id.btnCompare)
                compareButton?.setOnClickListener {
                    // Use the stored player data for comparison
                    lastSearchedPlayerData?.let {
                        showPlayerComparisonDialog(lastSearchedPlayerId ?: lastSearchedPlayerName ?: name)
                    }
                }
                
                // Add to the friends container
                friendsContainer.addView(newFriendView)
            }
        }
    }
    
    /**
     * Show dialog with found player name and compare button
     */
    private fun showPlayerFoundDialog(playerName: String, steamId: String) {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Player Found")
            .setMessage("Found player: $playerName")
            .setPositiveButton("Compare") { _, _ ->
                showPlayerComparisonDialog(steamId)
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        alertDialog.show()
    }
    
    /**
     * Extract formatted hero information from top heroes array
     */
    private fun extractTopHeroesInfo(heroesArray: JSONArray?): String {
        if (heroesArray == null || heroesArray.length() == 0) return "No hero data"
        
        val result = StringBuilder()
        for (i in 0 until minOf(3, heroesArray.length())) {
            val hero = heroesArray.optJSONObject(i)
            if (hero != null) {
                // Get hero ID - API returns numeric IDs
                val heroId = hero.optInt("hero_id", 0)
                // Get games count (API uses "games" not "matches")
                val games = hero.optInt("games", 0)
                // Get win count and calculate win rate
                val wins = hero.optInt("win", 0)
                val winRate = (wins.toFloat() / maxOf(games.toFloat(), 1f) * 100f)
                
                // Format with hero ID (ideally we'd have a hero name mapping)
                result.append("Hero $heroId: $games games, ${String.format("%.1f%%", winRate)} winrate")
                if (i < minOf(2, heroesArray.length() - 1)) {
                    result.append("\n")
                }
            }
        }
        
        return result.toString()
    }
}
