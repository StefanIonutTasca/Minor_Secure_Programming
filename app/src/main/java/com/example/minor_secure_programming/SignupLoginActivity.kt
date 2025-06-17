package com.example.minor_secure_programming

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.minor_secure_programming.utils.SupabaseManager
import kotlinx.coroutines.launch

class SignupLoginActivity : AppCompatActivity() {

    private var isLogin = true
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_login)

        // Views
        val titleText = findViewById<TextView>(R.id.titleText)
        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginSignupButton = findViewById<Button>(R.id.loginSignupButton)
        val toggleText = findViewById<TextView>(R.id.toggleText)
        val useBiometricsCheckbox = findViewById<CheckBox>(R.id.useBiometricsCheckbox)

        // Encrypted SharedPreferences
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            this,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // Autofill if stored
        val savedUsername = sharedPreferences.getString("username", null)
        val savedPassword = sharedPreferences.getString("password", null)

        savedUsername?.let { usernameInput.setText(it) }
        savedPassword?.let { passwordInput.setText(it) }

        // UI toggle
        fun updateUI() {
            titleText.text = if (isLogin) "Login" else "Sign Up"
            loginSignupButton.text = if (isLogin) "Login" else "Sign Up"
            toggleText.text = if (isLogin) "Don't have an account? Sign up" else "Already have an account? Login"
        }

        updateUI()

        // Setup biometric prompt
        biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Authentication successful", Toast.LENGTH_SHORT).show()
                    goToMain()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Auth error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
        )

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Cancel")
            .build()

        // Login/Signup logic
        loginSignupButton.setOnClickListener {
            val username = usernameInput.text.toString() // Using as email for Supabase auth
            val password = passwordInput.text.toString()
            val useBiometrics = useBiometricsCheckbox.isChecked

            if (username.isNotEmpty() && password.isNotEmpty()) {
                // Show loading indicator
                loginSignupButton.isEnabled = false
                loginSignupButton.text = if (isLogin) "Logging in..." else "Signing up..."
                
                // Store preferences for biometrics later
                if (useBiometrics) {
                    sharedPreferences.edit()
                        .putString("username", username)
                        .apply()
                }
                
                // Authenticate with Supabase
                if (isLogin) {
                    // Login
                    SupabaseManager.signIn(username, password, 
                        onSuccess = {
                            loginSignupButton.isEnabled = true
                            loginSignupButton.text = "Login"
                            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                            
                            // Handle biometric setup if needed
                            handleBiometricSetup(useBiometrics)
                        },
                        onError = { e ->
                            loginSignupButton.isEnabled = true
                            loginSignupButton.text = "Login"
                            Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    // Sign up
                    SupabaseManager.signUp(username, password,
                        onSuccess = {
                            loginSignupButton.isEnabled = true
                            loginSignupButton.text = "Sign Up"
                            Toast.makeText(this, "Sign up successful! Please verify your email if required.", Toast.LENGTH_LONG).show()
                            
                            // Switch to login mode
                            isLogin = true
                            updateUI()
                        },
                        onError = { e ->
                            loginSignupButton.isEnabled = true
                            loginSignupButton.text = "Sign Up"
                            Toast.makeText(this, "Sign up failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            } else {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            }
        }

        toggleText.setOnClickListener {
            isLogin = !isLogin
            updateUI()
        }
    }

    private fun goToMain() {
        startActivity(Intent(this@SignupLoginActivity, MainActivity::class.java))
        finish()
    }
    
    private fun handleBiometricSetup(useBiometrics: Boolean) {
        if (useBiometrics) {
            val biometricManager = BiometricManager.from(this)
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    biometricPrompt.authenticate(promptInfo)
                }
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Toast.makeText(this, "No biometrics enrolled. Please enroll or use password login.", Toast.LENGTH_LONG).show()
                    val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                    startActivity(enrollIntent)
                    goToMain() // Continue anyway
                }
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    Toast.makeText(this, "Biometric hardware not available.", Toast.LENGTH_LONG).show()
                    goToMain() // fallback
                }
                else -> {
                    Toast.makeText(this, "Biometric authentication not supported.", Toast.LENGTH_LONG).show()
                    goToMain() // fallback
                }
            }
        } else {
            // Proceed without biometrics
            goToMain()
        }
    }
}
