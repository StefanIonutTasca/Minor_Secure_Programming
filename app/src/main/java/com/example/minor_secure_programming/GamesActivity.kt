package com.example.minor_secure_programming

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView

data class GameInfo(
    val name: String,
    val category: String,
    val username: String
)

class GamesActivity : AppCompatActivity() {
    
    private lateinit var spinnerGameCategory: Spinner
    private lateinit var editTextSearch: EditText
    private lateinit var btnAddGame: Button
    private lateinit var btnMenu: ImageView
    
    private var userGames = mutableListOf<GameInfo>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_games)
        
        // Setup action bar with back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Games"
        
        // Initialize views
        initializeViews()
        
        // Load user games
        loadUserGames()
        
        // Setup functionality
        setupGameCategorySpinner()
        setupSearch()
        setupHeaderButtons()
        setupLolCard()
        setupBottomNavigation()
        
        // Initial display
        updateGameGrid()
    }
    
    private fun initializeViews() {
        spinnerGameCategory = findViewById(R.id.spinnerGameCategory)
        editTextSearch = findViewById(R.id.editTextSearch)
        btnAddGame = findViewById(R.id.btnAddGame)
        btnMenu = findViewById(R.id.btnMenu)
    }
    
    private fun loadUserGames() {
        userGames.clear()
        val sharedPref = getSharedPreferences("user_games", MODE_PRIVATE)
        val addedGames = sharedPref.all
        
        addedGames.forEach { (key, value) ->
            if (key.startsWith("game_")) {
                val gameName = key.substring(5)
                val username = value.toString()
                val category = determineGameCategory(gameName)
                userGames.add(GameInfo(gameName, category, username))
            }
        }
    }
    
    private fun determineGameCategory(gameName: String): String {
        return when {
            gameName.lowercase().contains("league of legends") -> "MOBA"
            gameName.lowercase().contains("dota") -> "MOBA"
            gameName.lowercase().contains("smite") -> "MOBA"
            gameName.lowercase().contains("heroes") -> "MOBA"
            gameName.lowercase().contains("valorant") -> "FPS"
            gameName.lowercase().contains("cs:go") -> "FPS"
            gameName.lowercase().contains("rainbow six") -> "FPS"
            gameName.lowercase().contains("overwatch") -> "FPS"
            gameName.lowercase().contains("apex") -> "Battle Royale"
            gameName.lowercase().contains("pubg") -> "Battle Royale"
            gameName.lowercase().contains("fortnite") -> "Battle Royale"
            else -> "Other"
        }
    }
    
    private fun setupGameCategorySpinner() {
        val categories = arrayOf("All Games", "MOBA", "FPS", "Battle Royale", "Strategy")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGameCategory.adapter = adapter
        spinnerGameCategory.setSelection(1) // Default to MOBA
        
        spinnerGameCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateGameGrid()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
    
    private fun setupSearch() {
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateGameGrid()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun setupHeaderButtons() {
        btnAddGame.setOnClickListener {
            startActivity(Intent(this, AddGameActivity::class.java))
        }
        
        btnMenu.setOnClickListener {
            showMenuOptions()
        }
    }
    
    private fun setupLolCard() {
        // LoL is the only hardcoded game since it has real stats
        findViewById<CardView>(R.id.cardLol).setOnClickListener {
            startActivity(Intent(this, LolStatsActivity::class.java))
        }
        findViewById<CardView>(R.id.cardLol).setOnLongClickListener {
            Toast.makeText(this, "League of Legends is the main game with working stats!", Toast.LENGTH_SHORT).show()
            true
        }
        
        // Setup New Game card
        findViewById<CardView>(R.id.cardNewGame).setOnClickListener {
            startActivity(Intent(this, AddGameActivity::class.java))
        }
    }
    
    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.navigation_wellness -> {
                    startActivity(Intent(this, WellnessActivity::class.java))
                    true
                }
                R.id.navigation_dashboard -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
    
    private fun updateGameGrid() {
        val gridLayout = findViewById<GridLayout>(R.id.gamesGrid)
        val selectedCategory = spinnerGameCategory.selectedItem.toString()
        val searchQuery = editTextSearch.text.toString().trim()
        
        // Filter games
        val filteredGames = userGames.filter { game ->
            val matchesCategory = selectedCategory == "All Games" || game.category == selectedCategory
            val matchesSearch = searchQuery.isEmpty() || game.name.lowercase().contains(searchQuery.lowercase())
            matchesCategory && matchesSearch
        }
        
        // Show/hide LoL based on filters
        val lolCard = findViewById<CardView>(R.id.cardLol)
        val lolMatchesCategory = selectedCategory == "All Games" || selectedCategory == "MOBA"
        val lolMatchesSearch = searchQuery.isEmpty() || "league of legends".contains(searchQuery.lowercase())
        lolCard.visibility = if (lolMatchesCategory && lolMatchesSearch) View.VISIBLE else View.GONE
        
        // Remove all dynamic game cards
        val cardsToRemove = mutableListOf<View>()
        for (i in 0 until gridLayout.childCount) {
            val child = gridLayout.getChildAt(i)
            if (child.tag == "user_game_card") {
                cardsToRemove.add(child)
            }
        }
        cardsToRemove.forEach { gridLayout.removeView(it) }
        
        // Add filtered user games
        val newGameCard = findViewById<CardView>(R.id.cardNewGame)
        val newGameIndex = gridLayout.indexOfChild(newGameCard)
        
        filteredGames.forEachIndexed { index, game ->
            val gameCard = createUserGameCard(game)
            gridLayout.addView(gameCard, newGameIndex + index)
        }
    }
    
    private fun createUserGameCard(game: GameInfo): CardView {
        val cardView = CardView(this).apply {
            tag = "user_game_card"
            radius = 12.dpToPx().toFloat()
            cardElevation = 4.dpToPx().toFloat()
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 160.dpToPx()
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
            }
        }
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
            gravity = android.view.Gravity.CENTER
        }
        
        // Game icon
        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(60.dpToPx(), 60.dpToPx()).apply {
                bottomMargin = 8.dpToPx()
            }
            setImageResource(android.R.drawable.ic_menu_gallery)
            setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
        }
        
        // Game name
        val nameText = TextView(this).apply {
            text = game.name
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#212121"))
            gravity = android.view.Gravity.CENTER
            maxLines = 2
        }
        
        // Username
        val usernameText = TextView(this).apply {
            text = "@${game.username}"
            textSize = 10f
            setTextColor(android.graphics.Color.parseColor("#757575"))
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 4.dpToPx() }
        }
        
        // Hint text
        val hintText = TextView(this).apply {
            text = "Long press to remove"
            textSize = 8f
            setTextColor(android.graphics.Color.parseColor("#999999"))
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 2.dpToPx() }
        }
        
        layout.addView(icon)
        layout.addView(nameText)
        layout.addView(usernameText)
        layout.addView(hintText)
        cardView.addView(layout)
        
        // Click listeners
        cardView.setOnClickListener {
            Toast.makeText(this, "${game.name} stats coming soon!\nUsername: ${game.username}", Toast.LENGTH_SHORT).show()
        }
        
        cardView.setOnLongClickListener {
            showRemoveGameDialog(game)
            true
        }
        
        return cardView
    }
    
    private fun showMenuOptions() {
        val options = arrayOf("View Added Games", "Clear All Games", "Refresh", "Settings")
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Menu Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddedGames()
                    1 -> clearAllGames()
                    2 -> refreshGames()
                    3 -> startActivity(Intent(this, SettingsActivity::class.java))
                }
            }
            .show()
    }
    
    private fun showAddedGames() {
        if (userGames.isEmpty()) {
            Toast.makeText(this, "No games added yet. Use the + button to add games!", Toast.LENGTH_LONG).show()
            return
        }
        
        val gamesList = StringBuilder("Added Games:\n\n")
        userGames.forEach { game ->
            gamesList.append("• ${game.name} (${game.category}) - @${game.username}\n")
        }
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Your Added Games")
            .setMessage(gamesList.toString())
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    
    private fun clearAllGames() {
        if (userGames.isEmpty()) {
            Toast.makeText(this, "No games to clear!", Toast.LENGTH_SHORT).show()
            return
        }
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Clear All Games")
            .setMessage("Are you sure you want to remove all added games?")
            .setPositiveButton("Clear All") { _, _ ->
                getSharedPreferences("user_games", MODE_PRIVATE).edit().clear().apply()
                userGames.clear()
                updateGameGrid()
                Toast.makeText(this, "All games cleared!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun refreshGames() {
        editTextSearch.setText("")
        spinnerGameCategory.setSelection(1) // MOBA
        loadUserGames()
        updateGameGrid()
        Toast.makeText(this, "Games refreshed!", Toast.LENGTH_SHORT).show()
    }
    
    private fun showRemoveGameDialog(game: GameInfo) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Remove Game")
            .setMessage("Remove \"${game.name}\" (Username: ${game.username}) from your games?")
            .setPositiveButton("Remove") { _, _ ->
                removeGame(game)
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }
    
    private fun removeGame(game: GameInfo) {
        val sharedPref = getSharedPreferences("user_games", MODE_PRIVATE)
        sharedPref.edit().remove("game_${game.name}").apply()
        userGames.remove(game)
        updateGameGrid()
        Toast.makeText(this, "\"${game.name}\" removed!", Toast.LENGTH_SHORT).show()
    }
    
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onResume() {
        super.onResume()
        loadUserGames()
        updateGameGrid()
    }
}
