package com.example.minor_secure_programming

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText

class AddGameActivity : AppCompatActivity() {
    
    private lateinit var spinnerGame: Spinner
    private lateinit var editTextUsername: TextInputEditText
    private lateinit var btnAddGame: Button
    
    private var selectedGame: String = ""
    private var username: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_game)
        
        // Setup action bar with back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add New Game"
        
        // Initialize views
        initializeViews()
        
        // Setup game spinner
        setupGameSpinner()
        
        // Setup username input
        setupUsernameInput()
        
        // Setup add game button
        setupAddGameButton()
    }
    
    private fun initializeViews() {
        spinnerGame = findViewById(R.id.spinnerGame)
        editTextUsername = findViewById(R.id.editTextUsername)
        btnAddGame = findViewById(R.id.btnAddGame)
    }
    
    private fun setupGameSpinner() {
        // Available games list categorized
        val games = arrayOf(
            "Select a game...",
            "--- MOBA Games ---",
            "League of Legends",
            "Dota 2", 
            "Smite",
            "Arena of Valor",
            "Heroes of Newerth",
            "Heroes of the Storm",
            "Mobile Legends",
            "Wild Rift",
            "--- FPS Games ---",
            "Valorant",
            "CS:GO",
            "Rainbow Six Siege",
            "Overwatch",
            "--- Battle Royale ---",
            "Apex Legends",
            "PUBG",
            "Fortnite"
        )
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, games)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGame.adapter = adapter
        
        spinnerGame.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = games[position]
                // Don't allow selection of headers (items starting with "---")
                selectedGame = if (position > 0 && !selectedItem.startsWith("---")) {
                    selectedItem
                } else {
                    ""
                }
                updateAddGameButton()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedGame = ""
                updateAddGameButton()
            }
        }
    }
    
    private fun setupUsernameInput() {
        editTextUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                username = s?.toString()?.trim() ?: ""
                updateAddGameButton()
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun setupAddGameButton() {
        btnAddGame.setOnClickListener {
            if (isFormValid()) {
                addGame()
            }
        }
    }
    
    private fun updateAddGameButton() {
        val isValid = isFormValid()
        
        btnAddGame.isEnabled = isValid
        btnAddGame.isClickable = isValid
        btnAddGame.isFocusable = isValid
        
        if (isValid) {
            btnAddGame.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            btnAddGame.setTextColor(Color.WHITE)
        } else {
            btnAddGame.setBackgroundColor(Color.parseColor("#CCCCCC"))
            btnAddGame.setTextColor(Color.parseColor("#999999"))
        }
    }
    
    private fun isFormValid(): Boolean {
        return selectedGame.isNotEmpty() && username.isNotEmpty() && username.length >= 3
    }
    
    private fun addGame() {
        // Save to SharedPreferences for persistence
        val sharedPref = getSharedPreferences("user_games", MODE_PRIVATE)
        val editor = sharedPref.edit()
        
        // Save the game with username (simple key-value storage)
        editor.putString("game_$selectedGame", username)
        editor.apply()
        
        // Show success message
        Toast.makeText(this, "Added $selectedGame with username: $username", Toast.LENGTH_LONG).show()
        
        // Return to games activity
        val intent = Intent(this, GamesActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 