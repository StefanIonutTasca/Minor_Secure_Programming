package com.example.minor_secure_programming

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONArray
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.random.Random

class CVActivity : AppCompatActivity() {
    
    // Game categories
    private val categories = mapOf(
        "FPS" to listOf("Rainbow Six Siege", "Valorant", "CS:GO", "Apex Legends"),
        "MOBA" to listOf("League of Legends", "Dota 2"),
        "MMO" to listOf("World of Warcraft", "Old School RuneScape"),
        "Battle Royale" to listOf("Fortnite", "Apex Legends"),
        "Sandbox" to listOf("Minecraft")
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cv)
        
        // Setup action bar with back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Gaming CV"
        
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
    private fun loadUserGames() {
        val sharedPrefs = getSharedPreferences("user_games", Context.MODE_PRIVATE)
        val gamesJson = sharedPrefs.getString("games", "[]") ?: "[]"
        val categoryGameCount = mutableMapOf<String, Int>()
        
        // Initialize counts for each category
        for (category in categories.keys) {
            categoryGameCount[category] = 0
        }
        
        try {
            val gamesArray = JSONArray(gamesJson)
            
            // Count games in each category
            for (i in 0 until gamesArray.length()) {
                val gameObj = gamesArray.getJSONObject(i)
                val gameName = gameObj.getString("name")
                
                // Find which category this game belongs to
                for ((category, games) in categories) {
                    if (games.contains(gameName)) {
                        categoryGameCount[category] = categoryGameCount[category]!! + 1
                    }
                }
            }
            
            // Update UI for each category with combined stats
            for (category in categories.keys) {
                // Find the card view for this category
                val categoryCard = findViewById<ViewGroup>(android.R.id.content).findViewWithTag<View>(category)
                
                if (categoryGameCount[category]!! > 0) {
                    // Generate aggregated stats for this category
                    populateCategoryStats(categoryCard, category, categoryGameCount[category]!!)
                } else {
                    // Set empty state for this category
                    val tvCategoryName = categoryCard.findViewById<TextView>(R.id.tvCategoryName)
                    val tvTotalHours = categoryCard.findViewById<TextView>(R.id.tvTotalHours)
                    val tvWinRate = categoryCard.findViewById<TextView>(R.id.tvWinRate)
                    val tvSkillLevel = categoryCard.findViewById<TextView>(R.id.tvSkillLevel)
                    val tvGamesCount = categoryCard.findViewById<TextView>(R.id.tvGamesCount)
                    val tvCategoryRank = categoryCard.findViewById<TextView>(R.id.tvCategoryRank)
                    val tvMastery = categoryCard.findViewById<TextView>(R.id.tvMastery)
                    
                    tvCategoryName.text = category
                    tvTotalHours.text = "-"
                    tvWinRate.text = "-"
                    tvSkillLevel.text = "-"
                    tvGamesCount.text = "0"
                    tvCategoryRank.text = "-"
                    tvMastery.text = "-"
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Populate category card with aggregated stats
     */
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
