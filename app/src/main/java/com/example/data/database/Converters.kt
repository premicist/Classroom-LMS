package com.example.data.database

import androidx.room.TypeConverter
import com.example.data.entity.AssignmentType
import com.example.data.entity.AttendanceStatus
import com.example.data.entity.HomeworkStatus
import com.example.data.entity.InterventionType
import com.example.data.entity.LiveAssessmentTaskType
import com.example.data.entity.MasteryLevel
import com.example.data.entity.SubmissionStatus

class Converters {
    @TypeConverter
    fun fromAssignmentType(value: AssignmentType?): String? = value?.name

    @TypeConverter
    fun toAssignmentType(value: String?): AssignmentType? = value?.let {
        try {
            AssignmentType.valueOf(it)
        } catch (e: Exception) {
            AssignmentType.HOMEWORK
        }
    }

    @TypeConverter
    fun fromSubmissionStatus(value: SubmissionStatus?): String? = value?.name

    @TypeConverter
    fun toSubmissionStatus(value: String?): SubmissionStatus? = value?.let {
        try {
            SubmissionStatus.valueOf(it)
        } catch (e: Exception) {
            SubmissionStatus.PENDING
        }
    }

    @TypeConverter
    fun fromHomeworkStatus(value: HomeworkStatus?): String? = value?.name

    @TypeConverter
    fun toHomeworkStatus(value: String?): HomeworkStatus? = value?.let {
        try {
            HomeworkStatus.valueOf(it)
        } catch (e: Exception) {
            HomeworkStatus.DONE
        }
    }

    @TypeConverter
    fun fromAttendanceStatus(value: AttendanceStatus?): String? = value?.name

    @TypeConverter
    fun toAttendanceStatus(value: String?): AttendanceStatus? = value?.let {
        try {
            AttendanceStatus.valueOf(it)
        } catch (e: Exception) {
            AttendanceStatus.PRESENT
        }
    }

    @TypeConverter
    fun fromInterventionType(value: InterventionType?): String? = value?.name

    @TypeConverter
    fun toInterventionType(value: String?): InterventionType? = value?.let {
        try {
            InterventionType.valueOf(it)
        } catch (e: Exception) {
            InterventionType.TUTORING
        }
    }

    @TypeConverter
    fun fromLiveAssessmentTaskType(value: LiveAssessmentTaskType?): String? = value?.name

    @TypeConverter
    fun toLiveAssessmentTaskType(value: String?): LiveAssessmentTaskType? = value?.let {
        try {
            LiveAssessmentTaskType.valueOf(it)
        } catch (e: Exception) {
            LiveAssessmentTaskType.CONCEPT_EXPLANATION
        }
    }

    @TypeConverter
    fun fromMasteryLevel(value: MasteryLevel?): String? = value?.name

    @TypeConverter
    fun toMasteryLevel(value: String?): MasteryLevel? = value?.let {
        try {
            MasteryLevel.valueOf(it)
        } catch (e: Exception) {
            MasteryLevel.MASTERED
        }
    }
}
