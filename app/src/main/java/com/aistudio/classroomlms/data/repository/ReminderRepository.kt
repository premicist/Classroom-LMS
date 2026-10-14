package com.aistudio.classroomlms.data.repository

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.aistudio.classroomlms.util.NotificationWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing reminders using WorkManager
 */
@Singleton
class ReminderRepository @Inject constructor(
    private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val UNIQUE_WORK_NAME_PREFIX = "lesson_plan_reminder_"
    }

    /**
     * Schedule a reminder for a lesson plan
     * @param lessonPlanId The ID of the lesson plan
     * @param triggerAtMillis The time to trigger the reminder (epoch millis)
     * @param title The notification title
     * @param message The notification message
     * @param classroomId The classroom ID (for context)
     */
    fun scheduleReminder(
        lessonPlanId: Long,
        triggerAtMillis: Long,
        title: String,
        message: String,
        classroomId: Long
    ) {
        // Cancel any existing reminder for this lesson plan first
        cancelReminder(lessonPlanId)

        val workName = "$UNIQUE_WORK_NAME_PREFIX$lessonPlanId"
        
        val workData = workDataOf(
            NotificationWorker.KEY_NOTIFICATION_TITLE to title,
            NotificationWorker.KEY_NOTIFICATION_MESSAGE to message,
            NotificationWorker.KEY_CLASSROOM_ID to classroomId,
            NotificationWorker.KEY_LESSON_PLAN_ID to lessonPlanId
        )

        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .addTag(UNIQUE_WORK_NAME_PREFIX)
            .setInputData(workData)
            .setInitialDelay(triggerAtMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Cancel a reminder for a lesson plan
     * @param lessonPlanId The ID of the lesson plan whose reminder to cancel
     */
    fun cancelReminder(lessonPlanId: Long) {
        val workName = "$UNIQUE_WORK_NAME_PREFIX$lessonPlanId"
        workManager.cancelUniqueWork(workName)
    }

    /**
     * Cancel all reminders
     */
    fun cancelAllReminders() {
        workManager.cancelAllWorkByTag(UNIQUE_WORK_NAME_PREFIX)
    }

    /**
     * Check if a reminder exists for a lesson plan
     * @param lessonPlanId The ID of the lesson plan
     * @return true if a reminder exists, false otherwise
     */
    suspend fun hasReminder(lessonPlanId: Long): Boolean {
        val workName = "$UNIQUE_WORK_NAME_PREFIX$lessonPlanId"
        return withContext(Dispatchers.IO) {
            workManager.getWorkInfosForUniqueWork(workName).get().any { workInfo ->
                workInfo.state == WorkInfo.State.ENQUEUED ||
                    workInfo.state == WorkInfo.State.RUNNING
            }
        }
    }
}