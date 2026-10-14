package com.aistudio.classroomlms.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aistudio.classroomlms.MainActivity
import com.aistudio.classroomlms.R
import com.aistudio.classroomlms.util.SafeLogger

/**
 * Worker for showing local notifications (reminders, alerts)
 */
class NotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    companion object {
        private const val TAG = "NotificationWorker"
        const val CHANNEL_ID = "lesson_reminders_channel"
        const val CHANNEL_NAME = "Lesson Reminders"

        const val KEY_NOTIFICATION_TITLE = "notification_title"
        const val KEY_NOTIFICATION_MESSAGE = "notification_message"
        const val KEY_NOTIFICATION_TYPE = "notification_type"
        const val KEY_CLASSROOM_ID = "classroom_id"
        const val KEY_LESSON_PLAN_ID = "lesson_plan_id"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
                if (existingChannel == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Notifications for lesson plan reminders and class alerts"
                    }
                    notificationManager.createNotificationChannel(channel)
                }
            }
        }
    }

    override fun doWork(): Result {
        val context = applicationContext
        createNotificationChannel(context)

        val title = inputData.getString(KEY_NOTIFICATION_TITLE)
            ?: "Classroom LMS Reminder"
        val message = inputData.getString(KEY_NOTIFICATION_MESSAGE)
            ?: "You have a reminder"
        val notificationType = inputData.getString(KEY_NOTIFICATION_TYPE)
            ?: "general"
        val classroomId = inputData.getLong(KEY_CLASSROOM_ID, -1)
        val lessonPlanId = inputData.getLong(KEY_LESSON_PLAN_ID, -1)

        SafeLogger.d(TAG, "Showing notification: $title - $message")

        // Create intent to open the app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            // Add extras for deep linking if needed
            putExtra("classroom_id", classroomId)
            putExtra("lesson_plan_id", lessonPlanId)
            putExtra("notification_type", notificationType)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
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

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())

        return Result.success()
    }
}