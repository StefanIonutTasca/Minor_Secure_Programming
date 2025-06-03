package com.example.minor_secure_programming

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SignupLoginActivity : AppCompatActivity() {

    private var isLogin = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_login)

        val titleText = findViewById<TextView>(R.id.titleText)
        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginSignupButton = findViewById<Button>(R.id.loginSignupButton)
        val toggleText = findViewById<TextView>(R.id.toggleText)

        fun updateUI() {
            titleText.text = if (isLogin) "Login" else "Sign Up"
            loginSignupButton.text = if (isLogin) "Login" else "Sign Up"
            toggleText.text = if (isLogin) "Don't have an account? Sign up" else "Already have an account? Login"
        }

        updateUI()

        loginSignupButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                val intent = Intent(this, DashboardActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            }
        }

        toggleText.setOnClickListener {
            isLogin = !isLogin
            updateUI()
        }
    }
}
