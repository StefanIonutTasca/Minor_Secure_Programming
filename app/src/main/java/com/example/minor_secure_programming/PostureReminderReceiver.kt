package com.example.minor_secure_programming

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * BroadcastReceiver for handling posture reminder alarms
 */
class PostureReminderReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "posture_reminder_channel"
        const val NOTIFICATION_ID = 1001
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        // Show the notification immediately
        if (intent.action == WellnessActivity.ACTION_POSTURE_REMINDER) {
            createNotificationChannel(context)
            showNotification(context)
            
            // Also broadcast the action in case the activity is already running
            val broadcastIntent = Intent(WellnessActivity.ACTION_POSTURE_REMINDER)
            context.sendBroadcast(broadcastIntent)
        }
    }
    
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Posture Reminders"
            val descriptionText = "Notifications for posture check reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun showNotification(context: Context) {
        // Create an intent for when user taps the notification
        val intent = Intent(context, WellnessActivity::class.java).apply {
            action = WellnessActivity.ACTION_POSTURE_REMINDER
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification with the app icon
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Posture Check")
            .setContentText("Time to check your posture and adjust if needed!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        try {
            // Show the notification
            with(NotificationManagerCompat.from(context)) {
                // Check for notification permission first (for Android 13+)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notify(NOTIFICATION_ID, builder.build())
                }
            }
        } catch (e: Exception) {
            // Just in case there's a permission issue
        }
    }
}
