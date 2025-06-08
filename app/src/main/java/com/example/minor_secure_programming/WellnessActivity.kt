package com.example.minor_secure_programming

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar
import java.util.concurrent.TimeUnit

class WellnessActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    
    companion object {
        private const val CHANNEL_ID = "posture_reminder_channel"
        private const val NOTIFICATION_ID = 1001
        private const val PERMISSION_REQUEST_CODE = 101
        private const val ALARM_REQUEST_CODE = 102
        const val ACTION_POSTURE_REMINDER = "com.example.minor_secure_programming.ACTION_POSTURE_REMINDER"
        private const val PREFS_NAME = "WellnessPrefs"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_REMINDER_INTERVAL = "reminder_interval"
        private const val KEY_HEALTH_POINTS = "healthPoints"
        private const val KEY_LAST_SESSION_DATE = "lastSessionDate"
        private const val KEY_SESSIONS_TODAY = "sessionsToday"
        private const val DEFAULT_HEALTH_POINTS = 100
        private const val SESSION_COMPLETION_REWARD = 15
        private const val MULTIPLE_SESSION_PENALTY = 20
        const val GAMING_CHANNEL_ID = "gaming_session_channel"
        const val GAMING_NOTIFICATION_ID = 2001
        const val ACTION_GAMING_SESSION_END = "com.example.minor_secure_programming.GAMING_SESSION_END"
        const val ACTION_GAMING_SESSION_OVERTIME = "com.example.minor_secure_programming.GAMING_SESSION_OVERTIME"
        private const val REQUEST_NOTIFICATION_PERMISSION = 123
        private const val OVERTIME_PENALTY_MINUTES = 30
        private const val OVERTIME_PENALTY_POINTS = 10
    }
    
    private var isSessionActive = false
    private var sessionStartTime: Long = 0
    private var eyeExerciseTimer: CountDownTimer? = null
    private var isEyeExerciseActive = false
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var switchPostureReminder: Switch
    private lateinit var spinnerReminderInterval: Spinner
    private lateinit var etSessionDuration: EditText
    private lateinit var tvRemainingTime: TextView
    private lateinit var tvHealthPoints: TextView
    private lateinit var tvSessionStatus: TextView
    private lateinit var btnStartSession: Button
    private lateinit var btnEndSession: Button
    private var gamingSessionTimer: CountDownTimer? = null
    private var healthPoints = DEFAULT_HEALTH_POINTS
    private var overtimePenaltyApplied = false
    private var sessionsToday = 0
    private var lastSessionDate = ""
    
    // Broadcast receiver for posture reminders
    private val postureReminderReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_POSTURE_REMINDER) {
                showPostureNotification()
                scheduleNextReminder() // Schedule next reminder
            }
        }
    }
    
    // Broadcast receiver for gaming session notifications
    private val gamingSessionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_GAMING_SESSION_END -> {
                    showGamingSessionEndedNotification()
                    endGamingSession()
                }
                ACTION_GAMING_SESSION_OVERTIME -> {
                    applyOvertimePenalty()
                    showGamingSessionOvertimeNotification()
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wellness)
        
        // Initialize gaming session UI elements
        etSessionDuration = findViewById(R.id.etSessionDuration)
        tvRemainingTime = findViewById(R.id.tvRemainingTime)
        tvHealthPoints = findViewById(R.id.tvHealthPoints)
        tvSessionStatus = findViewById(R.id.tvSessionStatus)
        btnStartSession = findViewById(R.id.btnStartSession)
        btnEndSession = findViewById(R.id.btnEndSession)
        
        // Set action bar title and enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Wellness"
        
        // Initialize shared preferences first
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Load health points and session data
        healthPoints = sharedPreferences.getInt(KEY_HEALTH_POINTS, DEFAULT_HEALTH_POINTS)
        lastSessionDate = sharedPreferences.getString(KEY_LAST_SESSION_DATE, "") ?: ""
        sessionsToday = if (isToday(lastSessionDate)) {
            sharedPreferences.getInt(KEY_SESSIONS_TODAY, 0)
        } else {
            0 // Reset if it's a new day
        }
        
        // Update display
        updateHealthPointsDisplay()
        updateSessionStatusDisplay()
        
        // Register the gaming session receiver
        val gamingIntentFilter = IntentFilter().apply {
            addAction(ACTION_GAMING_SESSION_END)
            addAction(ACTION_GAMING_SESSION_OVERTIME)
        }
        
        // On Android 13+, we need to specify the exported flag
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(gamingSessionReceiver, gamingIntentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(gamingSessionReceiver, gamingIntentFilter)
        }
        
        // Set up gaming session tracking buttons
        btnStartSession.setOnClickListener {
            startGamingSession()
        }
        
        btnEndSession.setOnClickListener {
            endGamingSession()
        }
        
        // Create notification channel for gaming sessions
        createGamingNotificationChannel()
        
        // Create notification channel for Android Oreo and above
        createNotificationChannel()
        
        // Register broadcast receiver for posture reminders
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(postureReminderReceiver, IntentFilter(ACTION_POSTURE_REMINDER), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(postureReminderReceiver, IntentFilter(ACTION_POSTURE_REMINDER))
        }
        
        // Set up posture reminder switch and spinner
        switchPostureReminder = findViewById(R.id.switchPostureReminder)
        val layoutReminderInterval = findViewById<View>(R.id.layoutReminderInterval)
        spinnerReminderInterval = findViewById(R.id.spinnerReminderInterval)
        
        // Populate the spinner with options
        val intervalOptions = arrayOf("10 seconds (test)", "15 minutes", "30 minutes", "45 minutes", "60 minutes")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, intervalOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerReminderInterval.adapter = adapter
        
        // Restore saved state
        val reminderEnabled = sharedPreferences.getBoolean(KEY_REMINDER_ENABLED, false)
        val reminderInterval = sharedPreferences.getInt(KEY_REMINDER_INTERVAL, 0)
        
        switchPostureReminder.isChecked = reminderEnabled
        layoutReminderInterval.visibility = if (reminderEnabled) View.VISIBLE else View.GONE
        spinnerReminderInterval.setSelection(reminderInterval)
        
        // Set up switch listener
        switchPostureReminder.setOnCheckedChangeListener { _, isChecked ->
            layoutReminderInterval.visibility = if (isChecked) View.VISIBLE else View.GONE
            
            // Save the state
            sharedPreferences.edit().putBoolean(KEY_REMINDER_ENABLED, isChecked).apply()
            
            if (isChecked) {
                // Request notification permission on Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            PERMISSION_REQUEST_CODE
                        )
                    } else {
                        schedulePostureReminder()
                    }
                } else {
                    schedulePostureReminder()
                }
            } else {
                cancelPostureReminders()
            }
        }
        
        spinnerReminderInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Save the selected interval
                sharedPreferences.edit().putInt(KEY_REMINDER_INTERVAL, position).apply()
                
                // Update the reminder schedule if enabled
                if (switchPostureReminder.isChecked) {
                    schedulePostureReminder()
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Nothing to do
            }
        }
        
        // Set up eye exercise button and progress bar
        val btnEyeExercise = findViewById<Button>(R.id.btnEyeExercise)
        val progressEyeExercise = findViewById<ProgressBar>(R.id.progressEyeExercise)
        
        btnEyeExercise.setOnClickListener {
            if (!isEyeExerciseActive) {
                isEyeExerciseActive = true
                btnEyeExercise.text = "Cancel Exercise Timer"
                
                // Start a 20-second countdown for the 20-20-20 rule
                eyeExerciseTimer = object : CountDownTimer(20000, 200) {
                    override fun onTick(millisUntilFinished: Long) {
                        val progress = 100 - (millisUntilFinished * 100 / 20000).toInt()
                        progressEyeExercise.progress = progress
                    }
                    
                    override fun onFinish() {
                        progressEyeExercise.progress = 100
                        btnEyeExercise.text = "Start Eye Exercise Timer"
                        isEyeExerciseActive = false
                    }
                }.start()
            } else {
                eyeExerciseTimer?.cancel()
                progressEyeExercise.progress = 0
                btnEyeExercise.text = "Start Eye Exercise Timer"
                isEyeExerciseActive = false
            }
        }
        
        // Initialize stats (these would be fetched from storage in a real app)
        val tvTotalPlaytime = findViewById<TextView>(R.id.tvTotalPlaytime)
        val tvBreaksTaken = findViewById<TextView>(R.id.tvBreaksTaken)
        val tvPostureReminders = findViewById<TextView>(R.id.tvPostureReminders)
        
        tvTotalPlaytime.text = "Total playtime this week: 0h 0m"
        tvBreaksTaken.text = "Breaks taken: 0"
        tvPostureReminders.text = "Posture reminders acknowledged: 0"
        
        // Bottom navigation removed
        // Navigation now uses navigation_wellness instead of navigation_lol
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isEyeExerciseActive) {
            eyeExerciseTimer?.cancel()
        }
        gamingSessionTimer?.cancel()
        try {
            unregisterReceiver(postureReminderReceiver)
            unregisterReceiver(gamingSessionReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }
    
    /**
     * Start the gaming session timer based on the user input
     */
    private fun startGamingSession() {
        val durationText = etSessionDuration.text.toString()
        if (durationText.isEmpty()) {
            Toast.makeText(this, "Please enter a session duration", Toast.LENGTH_SHORT).show()
            return
        }
        
        val durationMinutes = durationText.toIntOrNull() ?: 90 // Default to 90 if invalid input
        if (durationMinutes <= 0) {
            Toast.makeText(this, "Please enter a valid duration", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Check if this is an additional session today
        val today = getTodayAsString()
        if (sessionsToday > 0 && isToday(lastSessionDate)) {
            // Apply penalty for starting an additional session today
            healthPoints = maxOf(0, healthPoints - MULTIPLE_SESSION_PENALTY)
            updateHealthPointsDisplay()
            saveHealthPoints()
            
            Toast.makeText(
                this,
                "You've already had a gaming session today! -$MULTIPLE_SESSION_PENALTY points",
                Toast.LENGTH_LONG
            ).show()
        }
        
        // Update session tracking
        lastSessionDate = today
        sessionsToday += 1
        saveSessionTracking()
        updateSessionStatusDisplay()
        
        // Convert minutes to milliseconds
        val durationMillis = TimeUnit.MINUTES.toMillis(durationMinutes.toLong())
        
        // Cancel any existing timer
        gamingSessionTimer?.cancel()
        
        // Reset overtime penalty flag
        overtimePenaltyApplied = false
        
        // Start a new timer
        isSessionActive = true
        btnStartSession.isEnabled = false
        btnEndSession.isEnabled = true
        sessionStartTime = System.currentTimeMillis()
        
        gamingSessionTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) - 
                              TimeUnit.HOURS.toMinutes(hours)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - 
                              TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))
                
                val timeDisplay = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                tvRemainingTime.text = "Time remaining: $timeDisplay"
            }
            
            override fun onFinish() {
                showGamingSessionEndedNotification()
                
                // Start overtime tracking
                startOvertimeTracking()
            }
        }.start()
        
        Toast.makeText(this, "Gaming session started: $durationMinutes minutes", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * End the current gaming session and update UI
     */
    private fun endGamingSession() {
        if (!isSessionActive) return
        
        isSessionActive = false
        gamingSessionTimer?.cancel()
        gamingSessionTimer = null
        
        val sessionDuration = System.currentTimeMillis() - sessionStartTime
        val hours = TimeUnit.MILLISECONDS.toHours(sessionDuration)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(sessionDuration) - 
                      TimeUnit.HOURS.toMinutes(hours)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(sessionDuration) - 
                      TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(sessionDuration))
        
        tvRemainingTime.text = "Last session: ${hours}h ${minutes}m ${seconds}s"
        btnStartSession.isEnabled = true
        btnEndSession.isEnabled = false
        
        // Award points for properly ending a session
        if (!overtimePenaltyApplied) {
            healthPoints += SESSION_COMPLETION_REWARD
            Toast.makeText(this, 
                "You ended your session properly! +$SESSION_COMPLETION_REWARD wellness points", 
                Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Gaming session ended", Toast.LENGTH_SHORT).show()
        }
        
        // Save health points state
        saveHealthPoints()
    }
    
    /**
     * Start tracking overtime after the scheduled session ends
     */
    private fun startOvertimeTracking() {
        val overtimeMillis = TimeUnit.MINUTES.toMillis(OVERTIME_PENALTY_MINUTES.toLong())
        
        gamingSessionTimer = object : CountDownTimer(overtimeMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - 
                              TimeUnit.MINUTES.toSeconds(minutes)
                
                tvRemainingTime.text = "OVERTIME! ${minutes}m ${seconds}s until penalty"
            }
            
            override fun onFinish() {
                if (!overtimePenaltyApplied) {
                    showGamingSessionOvertimeNotification()
                    applyOvertimePenalty()
                }
            }
        }.start()
    }
    
    /**
     * Apply health points penalty for overtime gaming
     */
    private fun applyOvertimePenalty() {
        if (overtimePenaltyApplied) return
        
        healthPoints = maxOf(0, healthPoints - OVERTIME_PENALTY_POINTS)
        overtimePenaltyApplied = true
        updateHealthPointsDisplay()
        saveHealthPoints()
        
        Toast.makeText(
            this,
            "You've lost $OVERTIME_PENALTY_POINTS wellness points for excessive gaming!",
            Toast.LENGTH_LONG
        ).show()
    }
    
    /**
     * Update the health points display
     */
    private fun updateHealthPointsDisplay() {
        tvHealthPoints.text = "Wellness Points: $healthPoints"
    }
    
    /**
     * Update the session status display
     */
    private fun updateSessionStatusDisplay() {
        if (sessionsToday == 0) {
            tvSessionStatus.text = "No sessions today"
        } else {
            tvSessionStatus.text = "Sessions today: $sessionsToday"
        }
    }
    
    /**
     * Check if the given date string represents today
     */
    private fun isToday(dateStr: String): Boolean {
        if (dateStr.isEmpty()) return false
        
        val calendar = Calendar.getInstance()
        val today = String.format("%04d%02d%02d", 
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        return dateStr == today
    }
    
    /**
     * Get today's date as a string in format YYYYMMDD
     */
    private fun getTodayAsString(): String {
        val calendar = Calendar.getInstance()
        return String.format("%04d%02d%02d", 
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    
    /**
     * Save health points to shared preferences
     */
    private fun saveHealthPoints() {
        sharedPreferences.edit().putInt(KEY_HEALTH_POINTS, healthPoints).apply()
    }
    
    /**
     * Save session tracking data to shared preferences
     */
    private fun saveSessionTracking() {
        sharedPreferences.edit()
            .putString(KEY_LAST_SESSION_DATE, lastSessionDate)
            .putInt(KEY_SESSIONS_TODAY, sessionsToday)
            .apply()
    }
    
    /**
     * Create notification channel for gaming session notifications
     */
    private fun createGamingNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Gaming Sessions"
            val descriptionText = "Notifications for gaming session tracking"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(GAMING_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Show notification when gaming session ends
     */
    private fun showGamingSessionEndedNotification() {
        val intent = Intent(this, WellnessActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        
        val builder = NotificationCompat.Builder(this, GAMING_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Gaming Session Complete")
            .setContentText("Your scheduled gaming session has ended. Take a break!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                this@WellnessActivity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED) {
                notify(GAMING_NOTIFICATION_ID, builder.build())
            }
        }
    }
    
    /**
     * Show notification when user goes over the gaming time limit
     */
    private fun showGamingSessionOvertimeNotification() {
        val intent = Intent(this, WellnessActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        
        val builder = NotificationCompat.Builder(this, GAMING_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Overtime Gaming Alert")
            .setContentText("You've been gaming for too long! -$OVERTIME_PENALTY_POINTS wellness points")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                this@WellnessActivity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED) {
                notify(GAMING_NOTIFICATION_ID + 1, builder.build())
            }
        }
    }
    
    /**
     * Implementation of AdapterView.OnItemSelectedListener interface
     */
    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        // Handle item selection from spinner
        if (parent.id == R.id.spinnerReminderInterval) {
            val selectedValue = parent.getItemAtPosition(position).toString()
            // Save the selected reminder interval
            val interval = when (selectedValue) {
                "10 seconds (test)" -> 10_000L // Test interval
                "30 minutes" -> 30 * 60 * 1000L
                "1 hour" -> 60 * 60 * 1000L
                "2 hours" -> 2 * 60 * 60 * 1000L
                else -> 60 * 60 * 1000L // Default to 1 hour
            }
            sharedPreferences.edit().putLong(KEY_REMINDER_INTERVAL, interval).apply()
        }
    }
    
    /**
     * Implementation of AdapterView.OnItemSelectedListener interface
     */
    override fun onNothingSelected(parent: AdapterView<*>) {
        // Do nothing
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, schedule reminders
                if (switchPostureReminder.isChecked) {
                    schedulePostureReminder()
                }
            } else {
                // Permission denied
                Toast.makeText(
                    this,
                    "Notification permission is required for reminders",
                    Toast.LENGTH_LONG
                ).show()
                switchPostureReminder.isChecked = false
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Posture Reminder"
            val descriptionText = "Channel for posture reminder notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun schedulePostureReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, PostureReminderReceiver::class.java).apply {
            action = ACTION_POSTURE_REMINDER
        }
        
        // Cancel any existing alarms
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_NOTIFICATION_PERMISSION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        
        // Set up new alarm
        val intervalMillis = when(spinnerReminderInterval.selectedItemPosition) {
            0 -> 10000L // 10 seconds for testing
            1 -> TimeUnit.MINUTES.toMillis(15)
            2 -> TimeUnit.MINUTES.toMillis(30)
            3 -> TimeUnit.MINUTES.toMillis(45)
            else -> TimeUnit.MINUTES.toMillis(60)
        }
        
        // Get the display text for the toast
        val intervalText = if (spinnerReminderInterval.selectedItemPosition == 0) {
            "10 seconds"
        } else {
            val minutes = when(spinnerReminderInterval.selectedItemPosition) {
                1 -> 15
                2 -> 30
                3 -> 45
                else -> 60
            }
            "$minutes minutes"
        }
        val triggerTime = System.currentTimeMillis() + intervalMillis
        
        try {
            // First try to use exact alarms if allowed on API 31+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    // Fall back to inexact alarm
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // For API 23-30, try to use exact alarms
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } catch (e: SecurityException) {
                    // Fall back to inexact alarm if exact isn't allowed
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                // For older APIs
                try {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } catch (e: SecurityException) {
                    // Fall back to inexact alarm
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            }
            
            Toast.makeText(
                this,
                "Posture reminder set for $intervalText",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Couldn't set reminder: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun cancelPostureReminders() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, PostureReminderReceiver::class.java).apply {
            action = ACTION_POSTURE_REMINDER
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_NOTIFICATION_PERMISSION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        Toast.makeText(this, "Posture reminders turned off", Toast.LENGTH_SHORT).show()
    }
    
    private fun scheduleNextReminder() {
        // Only schedule next reminder if switch is still on
        if (switchPostureReminder.isChecked) {
            schedulePostureReminder()
        }
    }
    
    private fun showPostureNotification() {
        val intent = Intent(this, WellnessActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Posture Check")
            .setContentText("Time to check your posture and adjust if needed!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                this@WellnessActivity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED) {
                notify(NOTIFICATION_ID, builder.build())
                
                // Update stats
                val tvPostureReminders = findViewById<TextView>(R.id.tvPostureReminders)
                val currentCount = tvPostureReminders.text.toString().substringAfterLast(":").trim().toInt()
                val newCount = currentCount + 1
                tvPostureReminders.text = "Posture reminders acknowledged: $newCount"
            }
        }
    }
}
