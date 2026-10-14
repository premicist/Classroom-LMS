package com.aistudio.classroomlms.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aistudio.classroomlms.LmsApplication
import com.aistudio.classroomlms.R
import com.aistudio.classroomlms.data.entity.LessonPlanEntity
import com.aistudio.classroomlms.data.entity.LessonPlanEntity
import com.aistudio.classroomlms.data.AppDatabase
import com.aistudio.classroomlms.data.PlannerDao
import com.aistudio.classroomlms.util.SafeLogger
import java.util.List
import java.util.concurrent.Executors

/**
 * BroadcastReceiver to handle device boot completed and reschedule reminders
 */
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            SafeLogger.d(TAG, "Device boot completed, rescheduling reminders")
            
            // Use a background thread to avoid blocking the main thread
            Executors.newSingleThreadExecutor().execute {
                val appContext = context.applicationContext as LmsApplication
                val db = AppDatabase.getInstance(appContext)
                val plannerDao = db.plannerDao()
                
                try {
                    // Get all lesson plans with enabled reminders
                    val plansWithReminders: List<LessonPlanEntity> = plannerDao.getLessonPlansWithReminders()
                    
                    for (plan in plansWithReminders) {
                        if (plan.reminderEnabled && plan.reminderTime > 0) {
                            // Check if the reminder time is still in the future
                            if (plan.reminderTime > System.currentTimeMillis()) {
                                scheduleReminderNotification(
                                    appContext,
                                    plan.id ?: 0,
                                    plan.unitTitle,
                                    plan.reminderTime,
                                    plan.classroomId
                                )
                                SafeLogger.d(TAG, "Rescheduled reminder for lesson plan ${plan.id}")
                            } else {
                                SafeLogger.d(TAG, "Skipped reminder for lesson plan ${plan.id} - time has passed")
                            }
                        }
                    }
                } catch (e: Exception) {
                    SafeLogger.e(TAG, "Error rescheduling reminders on boot", e)
                }
            }
        }
    }

    private fun scheduleReminderNotification(
        context: Context,
        lessonPlanId: Long,
        title: String,
        reminderTime: Long,
        classroomId: Long
    ) {
        // Calculate trigger time
        val triggerTime = reminderTime
        
        // Only schedule if the reminder time is in the future
        if (triggerTime > System.currentTimeMillis()) {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = "ACTION_SHOW_REMINDER"
                putExtra("lesson_plan_id", lessonPlanId)
                putExtra("title", "Lesson Plan Reminder: $title")
                putExtra("message", "Your lesson plan '$title' is due now")
                putExtra("classroom_id", classroomId)
                putExtra("notification_type", "lesson_reminder")
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                lessonPlanId.toInt(), // Use lessonPlanId as request code for uniqueness
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            
            SafeLogger.d(TAG, "Scheduled reminder for lesson plan $lessonPlanId at $triggerTime")
        }
    }
}