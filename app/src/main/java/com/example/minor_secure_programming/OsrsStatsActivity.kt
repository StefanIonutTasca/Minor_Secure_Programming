package com.example.minor_secure_programming

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONArray

class OsrsStatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_osrs_stats)

        // ───── Action-bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "OSRS Stats"

        // ───── Username from intent
        val username = intent.getStringExtra("USERNAME") ?: "Unknown"
        findViewById<TextView>(R.id.usernameText).text = "Username: $username"

        // ───── Compare-all button
        findViewById<Button>(R.id.btnCompareStats).setOnClickListener {
            Toast.makeText(this, "Opening comparison view…", Toast.LENGTH_SHORT).show()
            findViewById<androidx.cardview.widget.CardView>(R.id.friendsCard).requestFocus()
        }

        // ───── Remove-game button
        findViewById<Button>(R.id.btnRemoveGame).setOnClickListener {
            showRemoveGameConfirmation("Old School RuneScape", username)
        }

        // ───── Search-compare button
        findViewById<Button>(R.id.btnSearch).setOnClickListener {
            val searchUsername = findViewById<EditText>(R.id.searchFriend).text.toString()
            if (searchUsername.isNotEmpty()) {
                showComparisonDialog(searchUsername)
            } else {
                Toast.makeText(this, "Please enter a username to compare with", Toast.LENGTH_SHORT).show()
            }
        }

        // ───── Hook up the hard-coded friend items’ buttons
        setupFriendCompareButtons()

        // ───── Bottom-navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java)); true
                }
                R.id.navigation_wellness -> true   // already here
                R.id.navigation_cv -> {
                    startActivity(Intent(this, CVActivity::class.java)); true
                }
                else -> false
            }
        }
        bottomNav.selectedItemId = R.id.navigation_wellness
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed(); return true
    }

    /*────────────────────────────────────────────────────────────────────────────*/

    /** Attach click-listeners to every “COMPARE” button inside friendsCard */
    private fun setupFriendCompareButtons() {
        val friendsCard = findViewById<androidx.cardview.widget.CardView>(R.id.friendsCard)
        val container = friendsCard.getChildAt(0) as LinearLayout

        // children: [TitleText, SearchRow, Friend1, Friend2, Friend3 …]
        // start from index 2
        for (i in 2 until container.childCount) {
            val row = container.getChildAt(i) as LinearLayout
            val compareBtn = row.getChildAt(row.childCount - 1) as Button
            val nameColumn = row.getChildAt(1) as LinearLayout
            val nameText = nameColumn.getChildAt(0) as TextView
            val friendUsername = nameText.text.toString()

            compareBtn.setOnClickListener { showComparisonDialog(friendUsername) }
        }
    }

    /** Confirmation dialog that requires username to match before removal */
    private fun showRemoveGameConfirmation(gameName: String, savedUsername: String) {
        val content = LayoutInflater.from(this)
            .inflate(R.layout.dialog_confirm_remove_game, null, false)
        val usernameInput = content.findViewById<EditText>(R.id.editTextConfirmUsername)

        AlertDialog.Builder(this)
            .setTitle("Remove $gameName")
            .setMessage("To confirm, please enter your username for this game.")
            .setView(content)
            .setPositiveButton("Remove") { _, _ ->
                if (usernameInput.text.toString() == savedUsername) {
                    removeGame(gameName, savedUsername)
                    Toast.makeText(this, "$gameName removed successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Username doesn’t match. Game not removed.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** Remove game entry from SharedPreferences */
    private fun removeGame(gameName: String, username: String) {
        val prefs = getSharedPreferences("user_games", Context.MODE_PRIVATE)
        val gamesJson = prefs.getString("games", "[]") ?: "[]"
        val newGames = JSONArray()

        val gamesArray = JSONArray(gamesJson)
        for (i in 0 until gamesArray.length()) {
            val game = gamesArray.getJSONObject(i)
            if (game.getString("name") != gameName || game.getString("username") != username) {
                newGames.put(game)
            }
        }
        prefs.edit().putString("games", newGames.toString()).apply()
    }

    /** Simple mock comparison dialog */
    private fun showComparisonDialog(opponentUsername: String) {
        AlertDialog.Builder(this)
            .setTitle("OSRS Stats Comparison")
            .setMessage(
                "You vs $opponentUsername\n\n" +
                        "Total Level: You (1500) vs $opponentUsername (1650)\n" +
                        "Combat Level: You (95) vs $opponentUsername (102)\n" +
                        "Top Skill: You (Fishing) vs $opponentUsername (Slayer)"
            )
            .setPositiveButton("Close", null)
            .setNeutralButton("View Full Details") { _, _ ->
                Toast.makeText(this, "Full comparison would open here", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
