package com.example.minor_secure_programming

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONArray

class ValorantStatsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_valorant_stats_new)
        
        // Setup action bar with back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Valorant Stats"
        
        // Get username from intent if available
        val username = intent.getStringExtra("username") ?: "User123"
        findViewById<TextView>(R.id.usernameText).text = "Username: $username"
        
        // Set up compare stats button click
        findViewById<Button>(R.id.btnCompareStats).setOnClickListener {
            Toast.makeText(this, "Opening comparison view...", Toast.LENGTH_SHORT).show()
            
            // In a real app, this would navigate to a comparison screen
            // For this demo, we'll just scroll to the friends section
            findViewById<androidx.cardview.widget.CardView>(R.id.friendsCard).requestFocus()
        }
        
        // Set up remove game button
        findViewById<Button>(R.id.btnRemoveGame).setOnClickListener {
            // Get the username from the intent extras
            val gameName = "Valorant"
            showRemoveGameConfirmation(gameName, username)
        }
        
        // Set up compare with new person button
        findViewById<Button>(R.id.btnSearch).setOnClickListener {
            val searchUsername = findViewById<EditText>(R.id.searchFriend).text.toString()
            if (searchUsername.isNotEmpty()) {
                // In a real app, this would fetch the user's stats and compare
                // For this demo, we'll show a comparison dialog
                showComparisonDialog(searchUsername)
            } else {
                Toast.makeText(this, "Please enter a username to compare with", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Set up listeners for all compare buttons in the friend list
        setupFriendCompareButtons()
        
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
                    // Already on wellness/stats page
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
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun setupFriendCompareButtons() {
        // For a real app, we would dynamically create these buttons or use RecyclerView
        // For this demo, we'll find all the buttons in the layout with a simple approach
        val friendsCard = findViewById<androidx.cardview.widget.CardView>(R.id.friendsCard)
        val linearLayout = friendsCard.getChildAt(0) as android.widget.LinearLayout
        
        // Start at index 2 to skip the title and search box
        for (i in 2 until linearLayout.childCount) {
            val friendLayout = linearLayout.getChildAt(i) as android.widget.LinearLayout
            // The compare button is the last child in each friend row
            val compareButton = friendLayout.getChildAt(friendLayout.childCount - 1) as Button
            val nameLayout = friendLayout.getChildAt(1) as android.widget.LinearLayout
            val nameText = nameLayout.getChildAt(0) as android.widget.TextView
            val username = nameText.text.toString()
            
            compareButton.setOnClickListener {
                showComparisonDialog(username)
            }
        }
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
                    // Go back to games page
                    finish()
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
    
    private fun showComparisonDialog(username: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Stats Comparison")
            .setMessage("Comparing your stats with $username:\n\n" +
                       "Matches: You (131) vs $username (156)\n" +
                       "Win Rate: You (52%) vs $username (57%)\n" +
                       "K/D/A: You (1.6/0.9/4.2) vs $username (1.9/0.7/5.3)\n" +
                       "Headshot %: You (38%) vs $username (42%)\n" +
                       "Favorite Agent: You (Jett) vs $username (Reyna)")
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("View Full Details") { _, _ ->
                Toast.makeText(this, "Full comparison details would open here", Toast.LENGTH_SHORT).show() 
            }
        builder.create().show()
    }
}
