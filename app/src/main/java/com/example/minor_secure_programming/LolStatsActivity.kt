package com.example.minor_secure_programming

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class LolStatsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lol_stats)
        
        // Setup action bar with back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "League of Legends Stats"
        
        // Set up compare stats button click
        findViewById<Button>(R.id.btnCompareStats).setOnClickListener {
            Toast.makeText(this, "Opening comparison view...", Toast.LENGTH_SHORT).show()
            
            // In a real app, this would navigate to a comparison screen
            // For this demo, we'll just scroll to the friends section
            findViewById<View>(R.id.friendsCard).requestFocus()
        }
        
        // Set up compare with new person button
        findViewById<Button>(R.id.btnSearch).setOnClickListener {
            val username = findViewById<EditText>(R.id.searchFriend).text.toString()
            if (username.isNotEmpty()) {
                // In a real app, this would fetch the user's stats and compare
                // For this demo, we'll show a comparison dialog
                showComparisonDialog(username)
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
                R.id.navigation_lol -> {
                    // Already on stats page
                    true
                }
                R.id.navigation_dashboard -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        
        // Set active navigation item
        bottomNav.selectedItemId = R.id.navigation_lol
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun setupFriendCompareButtons() {
        // For a real app, we would dynamically create these buttons or use RecyclerView
        // For this demo, we'll find all the buttons in the layout with a simple approach
        val friendsCard = findViewById<View>(R.id.friendsCard) as androidx.cardview.widget.CardView
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
    
    private fun showComparisonDialog(username: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Stats Comparison")
            .setMessage("Comparing your stats with $username:\n\n" +
                       "Wins: You (72) vs $username (85)\n" +
                       "Losses: You (53) vs $username (62)\n" +
                       "KDA: You (3.2) vs $username (3.8)\n" +
                       "CS/min: You (6.8) vs $username (7.2)\n" +
                       "Vision Score: You (22) vs $username (28)")
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("View Full Details") { _, _ ->
                Toast.makeText(this, "Full comparison details would open here", Toast.LENGTH_SHORT).show() 
            }
        builder.create().show()
    }
}
