package com.example.minor_secure_programming

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import org.json.JSONArray
import org.json.JSONObject

class GamesActivity : AppCompatActivity() {
    // List of available games with categories
    private val gameCategories = mapOf(
        "FPS" to listOf("Rainbow Six Siege", "Valorant", "CS:GO", "Apex Legends"),
        "MOBA" to listOf("League of Legends", "Dota 2"),
        "MMO" to listOf("World of Warcraft", "Old School RuneScape"),
        "Battle Royale" to listOf("Fortnite", "Apex Legends"),
        "Sandbox" to listOf("Minecraft")
    )
    
    // Flattened list of all games for the spinner
    private val availableGames = gameCategories.values.flatten().distinct().sorted().toTypedArray()
    
    // Container for user-added games
    private lateinit var gamesContainer: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_games)
        
        // Setup action bar with back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Games"
        
        // Get the LinearLayout container for games
        gamesContainer = findViewById<LinearLayout>(R.id.gamesContainer)
        
        // Setup Add Game button
        findViewById<MaterialButton>(R.id.btnAddGame).setOnClickListener {
            showAddGameDialog()
        }
        
        // Load any previously saved games
        loadSavedGames()
        
        // Click listeners are now set when creating game cards dynamically in loadSavedGames()
        
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
        
        // Don't set the active item here since it's a detail page
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    /**
     * Shows dialog for adding a new game
     */
    private fun showAddGameDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_game, null)
        
        // Set up category spinner
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.spinnerCategories)
        val categories = gameCategories.keys.toList()
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        categorySpinner.adapter = categoryAdapter
        
        // Set up game spinner
        val gameSpinner = dialogView.findViewById<Spinner>(R.id.spinnerGames)
        var gameAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, 
                      gameCategories[categories[0]]?.toTypedArray() ?: emptyArray())
        gameSpinner.adapter = gameAdapter
        
        // Update games when category changes
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                val gamesInCategory = gameCategories[selectedCategory]?.toTypedArray() ?: emptyArray()
                gameAdapter = ArrayAdapter(this@GamesActivity, 
                           android.R.layout.simple_spinner_dropdown_item, gamesInCategory)
                gameSpinner.adapter = gameAdapter
            }
            
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        
        // Create dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                // Get user inputs
                val selectedGame = gameSpinner.selectedItem?.toString() ?: ""
                val username = dialogView.findViewById<EditText>(R.id.editTextUsername).text.toString()
                
                // Validate inputs
                if (username.isNotEmpty() && selectedGame.isNotEmpty()) {
                    addGameToList(selectedGame, username)
                    saveGame(selectedGame, username)
                } else {
                    Toast.makeText(this, "Please enter a username and select a game", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
    }
    
    /**
     * Adds a game card to the UI
     */
    private fun addGameToList(gameName: String, username: String) {
        val cardView = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(120)
            ).apply {
                bottomMargin = dpToPx(16)
            }
            radius = dpToPx(8).toFloat()
            cardElevation = dpToPx(4).toFloat()
        }
        
        // Create constraint layout for the card content
        val constraintLayout = ConstraintLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // Add game logo
        val imageView = ImageView(this).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                dpToPx(60),
                dpToPx(60)
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginStart = dpToPx(16)
            }
            
            // Set appropriate game logo based on game name
            when (gameName) {
                "League of Legends" -> setImageResource(R.drawable.league)
                "Rainbow Six Siege" -> setImageResource(R.drawable.r6s)
                "Valorant" -> setImageResource(R.drawable.valorant)
                "Dota 2" -> setImageResource(R.drawable.dota2)
                "CS:GO", "Counter-Strike: Global Offensive" -> setImageResource(R.drawable.cs_go)
                else -> setImageResource(android.R.drawable.ic_menu_slideshow) // Generic icon for other games
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        constraintLayout.addView(imageView)
        
        // Add title
        val titleView = TextView(this).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                0,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                startToEnd = imageView.id
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                marginStart = dpToPx(16)
                marginEnd = dpToPx(16)
                topMargin = dpToPx(16)
            }
            text = gameName
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        }
        constraintLayout.addView(titleView)
        
        // Add username
        val usernameView = TextView(this).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                0,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                startToEnd = imageView.id
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToBottom = titleView.id
                marginStart = dpToPx(16)
                marginEnd = dpToPx(16)
                topMargin = dpToPx(8)
            }
            text = "Username: $username"
        }
        constraintLayout.addView(usernameView)
        
        // Add the view stats label
        val statsView = TextView(this).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginEnd = dpToPx(16)
                bottomMargin = dpToPx(8)
            }
            text = "Tap to view stats →"
            setTextColor(resources.getColor(android.R.color.holo_blue_dark))
        }
        constraintLayout.addView(statsView)
        
        // Add the layout to the card
        cardView.addView(constraintLayout)
        
        // Set click listener for specific games
        cardView.setOnClickListener {
            when (gameName) {
                "Rainbow Six Siege" -> {
                    val intent = Intent(this, R6StatsActivity::class.java)
                    intent.putExtra("USERNAME", username)
                    startActivity(intent)
                }
                "Valorant" -> {
                    val intent = Intent(this, ValorantStatsActivity::class.java)
                    intent.putExtra("USERNAME", username)
                    startActivity(intent)
                }
                "League of Legends" -> {
                    val intent = Intent(this, LolStatsActivity::class.java)
                    intent.putExtra("USERNAME", username)
                    startActivity(intent)
                }
                else -> {
                    Toast.makeText(this, "$gameName stats coming soon!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Add to the container (below the built-in game cards)
        gamesContainer.addView(cardView)
    }
    
    /**
     * Save game data to SharedPreferences
     */
    private fun saveGame(gameName: String, username: String) {
        val sharedPrefs = getSharedPreferences("user_games", Context.MODE_PRIVATE)
        val gamesJson = sharedPrefs.getString("games", "[]") ?: "[]"
        
        try {
            val gamesArray = JSONArray(gamesJson)
            
            // Find the game's category
            var gameCategory = ""
            for ((category, games) in gameCategories) {
                if (games.contains(gameName)) {
                    gameCategory = category
                    break
                }
            }
            
            val gameObject = JSONObject().apply {
                put("name", gameName)
                put("username", username)
                put("category", gameCategory)
            }
            gamesArray.put(gameObject)
            
            // Save updated array
            sharedPrefs.edit().putString("games", gamesArray.toString()).apply()
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving game data", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Load saved games from SharedPreferences
     */
    private fun loadSavedGames() {
        val sharedPrefs = getSharedPreferences("user_games", Context.MODE_PRIVATE)
        val gamesJson = sharedPrefs.getString("games", "[]") ?: "[]"
        
        try {
            // Clear any existing games first (except the add button and title)
            // Keep only the first two items (title and add button)
            while (gamesContainer.childCount > 2) {
                gamesContainer.removeViewAt(2)
            }
            
            // Create a map to collect games by category
            val gamesByCategory = mutableMapOf<String, MutableList<Pair<String, String>>>()
            
            val gamesArray = JSONArray(gamesJson)
            for (i in 0 until gamesArray.length()) {
                val game = gamesArray.getJSONObject(i)
                val name = game.getString("name")
                val username = game.getString("username")
                val category = game.optString("category", "Other")
                
                // Add to category map
                if (!gamesByCategory.containsKey(category)) {
                    gamesByCategory[category] = mutableListOf()
                }
                gamesByCategory[category]?.add(Pair(name, username))
            }
            
            // Now display games by category
            gamesByCategory.keys.sorted().forEach { category ->
                // Only add category if it has games
                val games = gamesByCategory[category]
                if (!games.isNullOrEmpty()) {
                    // Add category header
                    addCategoryHeader(category)
                    
                    // Add all games in this category
                    games.forEach { (name, username) ->
                        addGameToList(name, username)
                    }
                }
            }
            
            // Show a message if no games are added
            if (gamesArray.length() == 0) {
                val noGamesText = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text = "No games added yet. Click '+ Add New Game' to get started!"
                    textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    setPadding(0, dpToPx(32), 0, 0)
                }
                gamesContainer.addView(noGamesText)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading games: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Convert dp to pixels
     */
    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
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
            setTextColor(resources.getColor(android.R.color.holo_blue_dark))
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
     * Shows a confirmation dialog for removing a game that requires username verification
     */
    private fun showRemoveGameConfirmation(gameName: String, savedUsername: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_remove_game, null)
        val usernameInput = dialogView.findViewById<EditText>(R.id.editTextConfirmUsername)
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Remove $gameName")
            .setMessage("To confirm removal, please enter your username for this game")
            .setView(dialogView)
            .setPositiveButton("Remove") { _, _ -> 
                val enteredUsername = usernameInput.text.toString()
                if (enteredUsername == savedUsername) {
                    // Username matched, remove the game
                    removeGame(gameName, savedUsername)
                    Toast.makeText(this, "$gameName removed successfully", Toast.LENGTH_SHORT).show()
                    // Reload the games list
                    loadSavedGames()
                } else {
                    Toast.makeText(this, "Username doesn't match. Game not removed.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
    }
    
    /**
     * Remove a game from SharedPreferences
     */
    private fun removeGame(gameName: String, username: String) {
        val sharedPrefs = getSharedPreferences("user_games", Context.MODE_PRIVATE)
        val gamesJson = sharedPrefs.getString("games", "[]") ?: "[]"
        
        try {
            val gamesArray = JSONArray(gamesJson)
            val newGamesArray = JSONArray()
            
            // Copy all games except the one to be removed
            for (i in 0 until gamesArray.length()) {
                val game = gamesArray.getJSONObject(i)
                val name = game.getString("name")
                val gameUsername = game.getString("username")
                
                // Keep this game if it doesn't match the one to be removed
                if (name != gameName || gameUsername != username) {
                    newGamesArray.put(game)
                }
            }
            
            // Save updated array
            sharedPrefs.edit().putString("games", newGamesArray.toString()).apply()
        } catch (e: Exception) {
            Toast.makeText(this, "Error removing game: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
