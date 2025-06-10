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

class OverwatchStatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overwatch_stats)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Overwatch Stats"

        val battletag = intent.getStringExtra("USERNAME") ?: "Unknown#1234"
        findViewById<TextView>(R.id.usernameText).text = "Battletag: $battletag"

        findViewById<Button>(R.id.btnCompareStats).setOnClickListener {
            Toast.makeText(this, "Opening comparison view…", Toast.LENGTH_SHORT).show()
            findViewById<androidx.cardview.widget.CardView>(R.id.friendsCard).requestFocus()
        }

        findViewById<Button>(R.id.btnRemoveGame).setOnClickListener {
            showRemoveGameConfirmation("Overwatch", battletag)
        }

        findViewById<Button>(R.id.btnSearch).setOnClickListener {
            val friend = findViewById<EditText>(R.id.searchFriend).text.toString()
            if (friend.isNotEmpty()) showComparisonDialog(friend)
            else Toast.makeText(this, "Please enter a Battletag", Toast.LENGTH_SHORT).show()
        }

        setupFriendCompareButtons()

        findViewById<BottomNavigationView>(R.id.bottomNavigation).apply {
            setOnItemSelectedListener {
                when (it.itemId) {
                    R.id.navigation_home     -> { startActivity(Intent(this@OverwatchStatsActivity, MainActivity::class.java)); true }
                    R.id.navigation_wellness -> true
                    R.id.navigation_cv       -> { startActivity(Intent(this@OverwatchStatsActivity, CVActivity::class.java)); true }
                    else -> false
                }
            }
            selectedItemId = R.id.navigation_wellness
        }
    }

    override fun onSupportNavigateUp(): Boolean { onBackPressed(); return true }

    private fun setupFriendCompareButtons() {
        val friendsCard = findViewById<androidx.cardview.widget.CardView>(R.id.friendsCard)
        val container   = friendsCard.getChildAt(0) as LinearLayout
        for (i in 2 until container.childCount) {      // skip title + search row
            val row         = container.getChildAt(i) as LinearLayout
            val compareBtn  = row.getChildAt(row.childCount - 1) as Button
            val nameColumn  = row.getChildAt(1) as LinearLayout
            val name        = (nameColumn.getChildAt(0) as TextView).text.toString()
            compareBtn.setOnClickListener { showComparisonDialog(name) }
        }
    }

    private fun showRemoveGameConfirmation(game: String, savedUser: String) {
        val content      = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_remove_game, null)
        val input        = content.findViewById<EditText>(R.id.editTextConfirmUsername)

        AlertDialog.Builder(this)
            .setTitle("Remove $game")
            .setMessage("To confirm, enter your Battletag for this game.")
            .setView(content)
            .setPositiveButton("Remove") { _, _ ->
                if (input.text.toString() == savedUser) {
                    removeGame(game, savedUser)
                    Toast.makeText(this, "$game removed", Toast.LENGTH_SHORT).show()
                    finish()
                } else Toast.makeText(this, "Battletag doesn’t match.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeGame(game: String, user: String) {
        val prefs      = getSharedPreferences("user_games", Context.MODE_PRIVATE)
        val gamesArray = JSONArray(prefs.getString("games", "[]") ?: "[]")
        val newArray   = JSONArray()
        for (i in 0 until gamesArray.length()) {
            val g = gamesArray.getJSONObject(i)
            if (g.getString("name") != game || g.getString("username") != user) newArray.put(g)
        }
        prefs.edit().putString("games", newArray.toString()).apply()
    }

    private fun showComparisonDialog(opponent: String) {
        AlertDialog.Builder(this)
            .setTitle("Overwatch Stats Comparison")
            .setMessage(
                "You vs $opponent\n\n" +
                        "Games Played: 450 vs 520\n" +
                        "Win Rate: 51 % vs 55 %\n" +
                        "Elim/Deaths/Assists: 22/8/9 vs 25/7/11\n" +
                        "Most-Played Hero: Reinhardt vs Tracer"
            )
            .setPositiveButton("Close", null)
            .setNeutralButton("Full details") { _, _ ->
                Toast.makeText(this, "Full comparison would open here", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
