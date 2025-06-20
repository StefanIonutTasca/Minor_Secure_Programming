package com.example.minor_secure_programming

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import org.json.JSONObject
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.minor_secure_programming.utils.SupabaseManager
import kotlinx.coroutines.launch
import kotlin.random.Random

class CVActivity : AppCompatActivity() {
    
    // Game categories
    private val categories = mapOf(
        "FPS" to listOf("Rainbow Six Siege", "Valorant", "CS:GO", "Apex Legends", "Overwatch"),
        "MOBA" to listOf("League of Legends", "Dota 2"),
        "MMO" to listOf("World of Warcraft", "Old School RuneScape"),
        "Battle Royale" to listOf("Fortnite", "Apex Legends"),
        "Sandbox" to listOf("Minecraft")
    )
    
    // Game ids for Supabase database queries
    private val gameIds = mapOf(
        "Overwatch" to "971b54a5-a1fa-4531-b574-91331b7d95fe",
        "Dota 2" to "f7b06509-3dba-4755-aaf3-b25ed2a95fce"
        // Add other game ids as they become available
    )
    
    // Map of game names to categories
    private val gameToCategory = mutableMapOf<String, String>()
    
    // Combined stats tracking per category
    private data class CategoryStats(
        var totalHours: Int = 0,
        var gamesWon: Int = 0,
        var totalGames: Int = 0,
        var gameCount: Int = 0
    )
    
    // Store combined stats for each category
    private val categoryStats = mutableMapOf<String, CategoryStats>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cv)
        
        // Setup action bar with back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Gaming CV"
        
        // Initialize the game-to-category mapping
        initializeGameMapping()
        
        // Initialize category stats
        for (category in categories.keys) {
            categoryStats[category] = CategoryStats()
        }
        
        // Setup category sections
        setupCategoryViews()
        
        // Load user games
        loadUserGames()
        
        // Initialize the bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_wellness -> {
                    val intent = Intent(this, WellnessActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_cv -> {
                    // Already on CV page
                    true
                }
                else -> false
            }
        }
        
        // Set active navigation item
        bottomNav.selectedItemId = R.id.navigation_cv
    }
    
    private fun setupCategoryViews() {
        val containerLayout = findViewById<ConstraintLayout>(R.id.cvContainer)
        val headerView = findViewById<TextView>(R.id.tvCVHeader)
        headerView.text = "Gaming CV"
        
        var prevView: View = findViewById(R.id.tvCVDescription)
        
        // Update CV description
        findViewById<TextView>(R.id.tvCVDescription).text = "Your gaming statistics by category"
        
        for (category in categories.keys) {
            // Create a category stats card
            val categoryCard = LayoutInflater.from(this).inflate(
                R.layout.item_category_stats, 
                containerLayout, 
                false
            )
            
            // Set card ID and layout params
            categoryCard.id = View.generateViewId()
            val layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToBottom = prevView.id
                topMargin = dpToPx(16)
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                marginStart = dpToPx(16)
                marginEnd = dpToPx(16)
            }
            
            categoryCard.layoutParams = layoutParams
            containerLayout.addView(categoryCard)
            prevView = categoryCard
            
            // Tag to identify the card by category
            categoryCard.tag = category
        }
    }
    
    /**
     * Load user's saved games from SharedPreferences and populate the CV with combined stats
     */
    /**
     * Load user game stats from Supabase database and update the CV page
     */
    private fun loadUserGames() {
        val sharedPrefs = getSharedPreferences("user_games", Context.MODE_PRIVATE)
        val gamesJson = sharedPrefs.getString("games", "[]") ?: "[]"
        val categoryGameCount = mutableMapOf<String, Int>()
        
        try {
            // Find all category cards
            val containerLayout = findViewById<ConstraintLayout>(R.id.cvContainer)
            
            // Set up default placeholder content for each category
            for (i in 0 until containerLayout.childCount) {
                val view = containerLayout.getChildAt(i)
                
                // Check if this is a category card
                val category = view.tag as? String
                if (category != null) {
                    // For now, set placeholder content for each category card
                    val gameCount = categories[category]?.size ?: 0
                    populateCategoryStats(view, category, gameCount)
                }
            }
            
            // Load actual game stats from Supabase
            loadGameStatsFromSupabase()
            
        } catch (e: Exception) {
            Log.e("CVActivity", "Error loading user games", e)
            Snackbar.make(findViewById(android.R.id.content), 
                "Error loading game stats: ${e.message}", 
                Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun loadGameStatsFromSupabase() {
        lifecycleScope.launch {
            try {
                // First load Overwatch stats
                val overwatchId = gameIds["Overwatch"]
                if (overwatchId != null) {
                    val result = SupabaseManager.getGameStats(overwatchId)
                    if (result.isSuccess) {
                        val statsData = result.getOrNull()
                        if (statsData != null) {
                            // Process Overwatch stats
                            processOverwatchStats(statsData)
                        }
                    } else {
                        Log.e("CVActivity", "Failed to load Overwatch stats: ${result.exceptionOrNull()?.message}")
                    }
                }
                
                // Load Dota 2 stats
                val dotaId = gameIds["Dota 2"]
                if (dotaId != null) {
                    val result = SupabaseManager.getGameStats(dotaId)
                    if (result.isSuccess) {
                        val statsData = result.getOrNull()
                        if (statsData != null) {
                            // Process Dota 2 stats
                            processDotaStats(statsData)
                        }
                    }
                }
                
                // Process any additional game stats as they become available
                
            } catch (e: Exception) {
                Log.e("CVActivity", "Error loading stats from Supabase", e)
            }
        }
    }
    
    private fun processOverwatchStats(statsData: JSONObject) {
        try {
            // Parse JSON string to JSONObject if it's a string
            val statsJson = if (statsData.has("stats_data")) {
                val statsString = statsData.getString("stats_data")
                JSONObject(statsString)
            } else {
                statsData
            }
            
            // Extract profile data
            val profile = statsJson.optJSONObject("profile")
            if (profile != null) {
                val stats = profile.optJSONObject("stats")
                if (stats != null) {
                    val pc = stats.optJSONObject("pc")
                    if (pc != null) {
                        val quickplay = pc.optJSONObject("quickplay")
                        if (quickplay != null) {
                            // Get the heroes_comparisons data
                            val heroesComparisons = quickplay.optJSONObject("heroes_comparisons")
                            if (heroesComparisons != null) {
                                // Calculate play time in hours
                                var totalPlaytimeMs = 0L
                                val timePlayed = heroesComparisons.optJSONObject("time_played")
                                if (timePlayed != null) {
                                    val values = timePlayed.optJSONArray("values")
                                    if (values != null) {
                                        for (i in 0 until values.length()) {
                                            val heroTime = values.getJSONObject(i)
                                            totalPlaytimeMs += heroTime.optLong("value", 0)
                                        }
                                    }
                                }
                                 
                                // Convert milliseconds to hours
                                val totalHours = (totalPlaytimeMs / 3600000.0).toInt()
                                 
                                // Calculate win rate
                                var totalGames = 0
                                var gamesWon = 0
                                val gamesWonObj = heroesComparisons.optJSONObject("games_won")
                                if (gamesWonObj != null) {
                                    val gamesWonValues = gamesWonObj.optJSONArray("values")
                                    if (gamesWonValues != null) {
                                        for (i in 0 until gamesWonValues.length()) {
                                            val heroGames = gamesWonValues.getJSONObject(i)
                                            gamesWon += heroGames.optInt("value", 0)
                                        }
                                    }
                                }
                                
                                // Estimate total games from hero data
                                totalGames = gamesWon * 2 // Rough estimate if we don't have exact data
                                 
                                // Get win percentage from the data
                                val winPercentage = heroesComparisons.optJSONObject("win_percentage")
                                var avgWinRate = 0
                                var winRateCount = 0
                                if (winPercentage != null) {
                                    val winValues = winPercentage.optJSONArray("values")
                                    if (winValues != null) {
                                        for (i in 0 until winValues.length()) {
                                            val heroWinRate = winValues.getJSONObject(i)
                                            val rate = heroWinRate.optInt("value", 0)
                                            if (rate > 0) {
                                                avgWinRate += rate
                                                winRateCount++
                                            }
                                        }
                                    }
                                }
                                 
                                val finalWinRate = if (winRateCount > 0) avgWinRate / winRateCount else 0
                                
                                // Get player username
                                val summary = profile.optJSONObject("summary")
                                val username = summary?.optString("username", "Unknown")
                                
                                Log.d("CVActivity", "Processing Overwatch stats for $username: $totalHours hours, $finalWinRate% win rate, $gamesWon games won")
                                 
                                // Update category stats with Overwatch data
                                updateCategoryStats("Overwatch", totalHours, finalWinRate, gamesWon)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CVActivity", "Error processing Overwatch stats", e)
        }
    }
    
    private fun processDotaStats(statsData: JSONObject) {
        // Similar implementation for Dota stats could be added here
        // For now, we'll focus just on Overwatch
    }
    
    /**
     * Initialize mapping between games and their categories
     */
    private fun initializeGameMapping() {
        for ((category, games) in categories) {
            for (game in games) {
                gameToCategory[game] = category
            }
        }
    }
    
    /**
     * Update category stats with data from a game
     * This aggregates data across multiple games in the same category
     */
    private fun updateCategoryStats(game: String, hours: Int, winRate: Int, gamesCount: Int) {
        val category = gameToCategory[game] ?: return
        
        // Get or create category stats
        val stats = categoryStats[category] ?: CategoryStats()
        
        // Add this game's stats to the category total
        stats.totalHours += hours
        stats.gamesWon += gamesCount * (winRate / 100.0).toInt() // Approximate games won from win rate
        stats.totalGames += gamesCount
        stats.gameCount++
        
        // Store updated stats
        categoryStats[category] = stats
        
        // Calculate the aggregated win rate for the category
        val categoryWinRate = if (stats.totalGames > 0) {
            (stats.gamesWon * 100) / stats.totalGames
        } else {
            0
        }
        
        // Update the category UI
        updateCategoryUI(category, stats.totalHours, categoryWinRate, stats.totalGames)
    }
    
    /**
     * Update the UI for a category with combined stats
     */
    private fun updateCategoryUI(category: String, hours: Int, winRate: Int, gamesCount: Int) {
        val containerLayout = findViewById<ConstraintLayout>(R.id.cvContainer)
        val categoryCard = containerLayout.findViewWithTag<View>(category)
        
        if (categoryCard != null) {
            // Determine skill level based on win rate
            val skillLevel = when {
                winRate > 60 -> "High"
                winRate > 50 -> "Medium"
                else -> "Developing"
            }
            
            // Determine category rank based on win rate
            val categoryRanks = listOf("S", "A+", "A", "B+", "B", "C+", "C")
            val categoryRank = when {
                winRate > 60 -> categoryRanks[0]
                winRate > 57 -> categoryRanks[1]
                winRate > 54 -> categoryRanks[2]
                winRate > 51 -> categoryRanks[3]
                winRate > 48 -> categoryRanks[4]
                winRate > 45 -> categoryRanks[5]
                else -> categoryRanks[6]
            }
            
            // Determine mastery level based on hours played
            val masteryLevels = listOf("Novice", "Intermediate", "Advanced", "Expert", "Master")
            val masteryIndex = when {
                hours > 1000 -> 4
                hours > 500 -> 3
                hours > 250 -> 2
                hours > 100 -> 1
                else -> 0
            }
            
            // Update the UI on the main thread
            runOnUiThread {
                val tvCategoryName = categoryCard.findViewById<TextView>(R.id.tvCategoryName)
                val tvTotalHours = categoryCard.findViewById<TextView>(R.id.tvTotalHours)
                val tvWinRate = categoryCard.findViewById<TextView>(R.id.tvWinRate)
                val tvSkillLevel = categoryCard.findViewById<TextView>(R.id.tvSkillLevel)
                val tvGamesCount = categoryCard.findViewById<TextView>(R.id.tvGamesCount)
                val tvCategoryRank = categoryCard.findViewById<TextView>(R.id.tvCategoryRank)
                val tvMastery = categoryCard.findViewById<TextView>(R.id.tvMastery)
                
                tvCategoryName.text = category
                tvTotalHours.text = hours.toString()
                tvWinRate.text = "$winRate%"
                tvSkillLevel.text = skillLevel
                tvGamesCount.text = gamesCount.toString()
                tvCategoryRank.text = categoryRank
                tvMastery.text = masteryLevels[masteryIndex]
                
                // Log the update for debugging
                Log.d("CVActivity", "Updated $category stats - Hours: $hours, Win Rate: $winRate%, Games: $gamesCount")
            }
        }
    }
    
    private fun populateCategoryStats(cardView: View, category: String, gameCount: Int) {
        val tvCategoryName = cardView.findViewById<TextView>(R.id.tvCategoryName)
        val tvTotalHours = cardView.findViewById<TextView>(R.id.tvTotalHours)
        val tvWinRate = cardView.findViewById<TextView>(R.id.tvWinRate)
        val tvSkillLevel = cardView.findViewById<TextView>(R.id.tvSkillLevel)
        val tvGamesCount = cardView.findViewById<TextView>(R.id.tvGamesCount)
        val tvCategoryRank = cardView.findViewById<TextView>(R.id.tvCategoryRank)
        val tvMastery = cardView.findViewById<TextView>(R.id.tvMastery)
        
        // Set category name
        tvCategoryName.text = category
        
        // For demo purposes, generate some realistic looking stats
        // In a real app, these would be calculated from actual game data
        val totalHours = when (category) {
            "FPS" -> Random.nextInt(100, 500)
            "MOBA" -> Random.nextInt(200, 1000)
            "MMO" -> Random.nextInt(500, 2000)
            "Battle Royale" -> Random.nextInt(50, 300)
            else -> Random.nextInt(20, 200)
        }
        
        val winRate = Random.nextInt(40, 65)
        
        val skillLevel = when {
            winRate > 60 -> "High"
            winRate > 50 -> "Medium"
            else -> "Developing"
        }
        
        val categoryRanks = listOf("S", "A+", "A", "B+", "B", "C+", "C")
        val categoryRank = when {
            winRate > 60 -> categoryRanks[0]
            winRate > 57 -> categoryRanks[1]
            winRate > 54 -> categoryRanks[2]
            winRate > 51 -> categoryRanks[3]
            winRate > 48 -> categoryRanks[4]
            winRate > 45 -> categoryRanks[5]
            else -> categoryRanks[6]
        }
        
        val masteryLevels = listOf("Novice", "Intermediate", "Advanced", "Expert", "Master")
        val masteryIndex = when {
            totalHours > 1000 -> 4
            totalHours > 500 -> 3
            totalHours > 250 -> 2
            totalHours > 100 -> 1
            else -> 0
        }
        
        // Set the values
        tvTotalHours.text = "$totalHours"
        tvWinRate.text = "$winRate%"
        tvSkillLevel.text = skillLevel
        tvGamesCount.text = gameCount.toString()
        tvCategoryRank.text = categoryRank
        tvMastery.text = masteryLevels[masteryIndex]
    }
    
    // No longer need GameAdapter or GameItem classes since we're showing aggregate stats
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    /**
     * Convert dp to pixels
     */
    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
