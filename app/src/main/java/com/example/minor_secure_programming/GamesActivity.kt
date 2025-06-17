package com.example.minor_secure_programming

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.example.minor_secure_programming.models.Game
import com.example.minor_secure_programming.models.GameCategory
import com.example.minor_secure_programming.utils.SupabaseManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class GamesActivity : AppCompatActivity() {
    // Container for user-added games
    private lateinit var gamesContainer: LinearLayout
    
    // Progress indicator
    private lateinit var progressBar: ProgressBar
    
    // Lists for game data
    private var gameCategories = listOf<GameCategory>()
    private var userGames = listOf<Game>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_games)
        
        // Setup action bar with back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Games"
        
        // Get the LinearLayout container for games and progress bar
        gamesContainer = findViewById<LinearLayout>(R.id.gamesContainer)
        progressBar = findViewById(R.id.progressBarGames)
        
        // Setup Add Game button
        findViewById<MaterialButton>(R.id.btnAddGame).setOnClickListener {
            fetchGameCategories { categories ->
                if (categories.isNotEmpty()) {
                    showAddGameDialog(categories)
                } else {
                    Toast.makeText(this, "Cannot add games at this time. Please try again later.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Load games from Supabase
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
                    val intent = Intent(this, CVActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    /**
     * Add a new game to Supabase
     */
    private fun addGameToSupabase(categoryId: String, gameName: String, username: String) {
        // Show progress
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val success = SupabaseManager.addGame(categoryId, gameName, username)
                
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    
                    if (success) {
                        Toast.makeText(this@GamesActivity, "Game added successfully!", Toast.LENGTH_SHORT).show()
                        // Refresh the games list
                        loadUserGames()
                    } else {
                        Toast.makeText(this@GamesActivity, "Failed to add game", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@GamesActivity, "Error adding game: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Fetch game categories from Supabase
     */
    private fun fetchGameCategories(callback: (List<GameCategory>) -> Unit) {
        lifecycleScope.launch {
            try {
                val categories = SupabaseManager.getGameCategories()
                gameCategories = categories
                runOnUiThread { callback(categories) }
            } catch (e: Exception) {
                Log.e("GamesActivity", "Error fetching game categories: ${e.message}", e)
                runOnUiThread { 
                    Toast.makeText(this@GamesActivity, "Error loading game categories", Toast.LENGTH_SHORT).show()
                    callback(emptyList())
                }
            }
        }
    }
    
    /**
     * Load user games from Supabase
     */
    private fun loadUserGames() {
        // Show loading indicator
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val games = SupabaseManager.getUserGames()
                userGames = games
                
                runOnUiThread {
                    // Hide loading indicator
                    progressBar.visibility = View.GONE
                    
                    // Since we're directly accessing the layout components without recreating them,
                    // we should clear the container except for the first 3 items (title, progress bar, add button)
                    val childCount = gamesContainer.childCount
                    if (childCount > 3) {
                        gamesContainer.removeViews(3, childCount - 3)
                    }
                    
                    // Group games by category
                    val gamesByCategory = games.groupBy { it.category_name }
                    
                    if (games.isEmpty()) {
                        val noGamesText = TextView(this@GamesActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, dpToPx(16), 0, 0) }
                            text = "You haven't added any games yet."
                            textSize = 16f
                            gravity = android.view.Gravity.CENTER
                        }
                        gamesContainer.addView(noGamesText)
                    } else {
                        // Sort categories alphabetically
                        val sortedCategories = gamesByCategory.keys.sorted()
                        
                        // For each category, add a header and then all games in that category
                        sortedCategories.forEach { category ->
                            val gamesInCategory = gamesByCategory[category] ?: return@forEach
                            if (gamesInCategory.isNotEmpty()) {
                                addCategoryHeader(category)
                                
                                // Sort games within each category by name
                                val sortedGames = gamesInCategory.sortedBy { it.name }
                                sortedGames.forEach { game ->
                                    addGameToList(game)
                                }
                            }
                        }
                    }
                    
                    // Show a toast with the count of games loaded
                    if (games.isNotEmpty()) {
                        Toast.makeText(
                            this@GamesActivity, 
                            "Loaded ${games.size} game${if (games.size == 1) "" else "s"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    
                    // Show error message
                    val errorMessage = "Error loading games: ${e.message}"
                    Toast.makeText(this@GamesActivity, errorMessage, Toast.LENGTH_LONG).show()
                    Log.e("GamesActivity", errorMessage, e)
                    
                    // Clear the container except for the first 3 items
                    val childCount = gamesContainer.childCount
                    if (childCount > 3) {
                        gamesContainer.removeViews(3, childCount - 3)
                    }
                    
                    // Add error message text
                    val noGamesText = TextView(this@GamesActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { setMargins(0, dpToPx(16), 0, 0) }
                        text = "Error loading games. Please try again later."
                        textSize = 16f
                        gravity = android.view.Gravity.CENTER
                    }
                    gamesContainer.addView(noGamesText)
                }
            }
        }
    }
    
    /**
     * Map of predefined games by category
     */
    private val predefinedGames = mapOf(
        "FPS" to listOf("Overwatch", "Call of Duty", "Valorant", "Counter-Strike"),
        "MOBA" to listOf("League of Legends", "Dota 2", "Heroes of the Storm", "Smite"),
        "Strategy" to listOf("StarCraft II", "Age of Empires", "Civilization VI", "Warcraft III")
    )
    
    /**
     * Show dialog to add a new game
     */
    private fun showAddGameDialog(categories: List<GameCategory>) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_game, null)
        val usernameField = dialogView.findViewById<EditText>(R.id.editTextUsername)
        
        // Game categories spinner
        val categoriesSpinner = dialogView.findViewById<Spinner>(R.id.spinnerGameCategory)
        val categoryNames = categories.map { it.name }.toTypedArray()
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryNames)
        categoriesSpinner.adapter = categoryAdapter
        
        // Game names spinner
        val gamesSpinner = dialogView.findViewById<Spinner>(R.id.spinnerGameName)
        // Using ArrayList instead of array to support modifications
        val gamesList = ArrayList<String>()
        val gamesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, gamesList)
        gamesSpinner.adapter = gamesAdapter
        
        // Update game list when category changes
        categoriesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categoryNames[position]
                val games = predefinedGames[selectedCategory] ?: emptyList()
                
                // Replace the contents of the adapter properly
                gamesAdapter.clear()
                for (game in games) {
                    gamesAdapter.add(game)
                }
                gamesAdapter.notifyDataSetChanged()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                gamesAdapter.clear()
                gamesAdapter.notifyDataSetChanged()
            }
        }
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Add a New Game")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val selectedCategoryIndex = categoriesSpinner.selectedItemPosition
                val categoryId = if (selectedCategoryIndex >= 0 && selectedCategoryIndex < categories.size) {
                    categories[selectedCategoryIndex].id
                } else {
                    "" // default empty category ID as fallback
                }
                
                val selectedCategory = if (selectedCategoryIndex >= 0) categoryNames[selectedCategoryIndex] else ""
                val selectedGameIndex = gamesSpinner.selectedItemPosition
                val gameName = if (selectedGameIndex >= 0 && predefinedGames[selectedCategory] != null) {
                    predefinedGames[selectedCategory]!![selectedGameIndex]
                } else {
                    ""
                }
                
                val username = usernameField.text.toString().trim()
                
                if (username.isNotEmpty() && gameName.isNotEmpty() && categoryId.isNotEmpty()) {
                    addGameToSupabase(categoryId, gameName, username)
                } else {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
    }
    
    /**
     * Convert dp to pixels
     */
    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
    
    /**
     * Adds a game item to the UI from a Supabase Game model
     */
    private fun addGameToList(game: Game) {
        // Create card for the game
        val gameCard = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(8), 0, dpToPx(8))
            }
            radius = dpToPx(8).toFloat()
            cardElevation = dpToPx(2).toFloat()
            
            // Make the card clickable for supported games
            when (game.name) {
                "Dota 2" -> {
                    isClickable = true
                    isFocusable = true
                    foreground = getDrawable(android.R.drawable.list_selector_background)
                    setOnClickListener {
                        val intent = Intent(this@GamesActivity, DotaStatsActivity::class.java)
                        // Pass any necessary game data
                        intent.putExtra("GAME_ID", game.id)
                        intent.putExtra("USERNAME", game.username)
                        startActivity(intent)
                    }
                }
                "Overwatch" -> {
                    isClickable = true
                    isFocusable = true
                    foreground = getDrawable(android.R.drawable.list_selector_background)
                    setOnClickListener {
                        val intent = Intent(this@GamesActivity, OverwatchStatsActivity::class.java)
                        // Pass any necessary game data
                        intent.putExtra("GAME_ID", game.id)
                        intent.putExtra("USERNAME", game.username)
                        startActivity(intent)
                    }
                }
            }
        }
        
        // Card content layout
        val cardContent = ConstraintLayout(this).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }
        
        // Game name text
        val gameName = TextView(this).apply {
            id = View.generateViewId()
            text = game.name
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = cardContent.id
                startToStart = cardContent.id
            }
        }
        cardContent.addView(gameName)
        
        // Username text
        val usernameText = TextView(this).apply {
            id = View.generateViewId()
            text = "Username: ${game.username}"
            textSize = 14f
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToBottom = gameName.id
                startToStart = cardContent.id
                topMargin = dpToPx(4)
            }
        }
        cardContent.addView(usernameText)
        
        // Remove button
        val removeButton = MaterialButton(this).apply {
            id = View.generateViewId()
            text = "Remove"
            setBackgroundColor(resources.getColor(android.R.color.holo_red_light, theme))
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT, 
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = cardContent.id
                endToEnd = cardContent.id
            }
            setOnClickListener {
                showRemoveGameConfirmation(game)
            }
        }
        cardContent.addView(removeButton)
        
        gameCard.addView(cardContent)
        gamesContainer.addView(gameCard)
    }
    
    /**
     * Adds a category header to the UI
     */
    private fun addCategoryHeader(category: String) {
        val headerView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(16), 0, dpToPx(8))
            }
            text = category
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(resources.getColor(android.R.color.holo_blue_dark, theme))
        }
        gamesContainer.addView(headerView)
        
        // Add a divider
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            ).apply {
                setMargins(0, dpToPx(4), 0, dpToPx(8))
            }
            setBackgroundColor(resources.getColor(android.R.color.darker_gray))
        }
        gamesContainer.addView(divider)
    }
    
    /**
     * Shows a confirmation dialog for removing a game
     */
    private fun showRemoveGameConfirmation(game: Game) {
        AlertDialog.Builder(this)
            .setTitle("Remove ${game.name}")
            .setMessage("Are you sure you want to remove ${game.name} (${game.username})?")
            .setPositiveButton("Remove") { _, _ ->
                removeGameFromSupabase(game)
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }
    
    /**
     * Remove a game from Supabase database
     */
    private fun removeGameFromSupabase(game: Game) {
        // Show loading indicator
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val success = SupabaseManager.deleteGame(game.id)
                
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    
                    if (success) {
                        Toast.makeText(this@GamesActivity, "${game.name} removed successfully", Toast.LENGTH_SHORT).show()
                        // Reload games from Supabase
                        loadUserGames()
                    } else {
                        Toast.makeText(this@GamesActivity, "Failed to remove game", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@GamesActivity, "Error removing game: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
