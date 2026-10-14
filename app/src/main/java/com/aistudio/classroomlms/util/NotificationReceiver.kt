package com.aistudio.classroomlms.util

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aistudio.classroomlms.R
import com.aistudio.classroomlms.util.SafeLogger

/**
 * BroadcastReceiver to handle reminder alarms and show notifications
 */
class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
        const val CHANNEL_ID = "lesson_reminders_channel"
        const val CHANNEL_NAME = "Lesson Reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "ACTION_SHOW_REMINDER") {
            val title = intent.getStringExtra("title") ?: "Classroom LMS Reminder"
            val message = intent.getStringExtra("message") ?: "You have a reminder"
            val notificationType = intent.getStringExtra("notification_type") ?: "general"
            val classroomId = intent.getLongExtra("classroom_id", -1)
            val lessonPlanId = intent.getLongExtra("lesson_plan_id", -1)

            SafeLogger.d(TAG, "Received reminder: $title - $message")
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                SafeLogger.w(TAG, "Notification permission is not granted; reminder cannot be shown")
                return
            }

            // Create notification channel if needed (for Android O and above)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for lesson plan reminders and class alerts"
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.createNotificationChannel(channel)
            }

            // Create intent to open the app
            val appIntent = Intent(context, com.aistudio.classroomlms.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("classroom_id", classroomId)
                putExtra("lesson_plan_id", lessonPlanId)
                putExtra("notification_type", notificationType)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = NotificationManagerCompat.from(context)
            // Use a unique ID for each notification (e.g., lessonPlanId) to avoid overwriting
            notificationManager.notify(lessonPlanId.toInt(), builder.build())
        }
    }
}