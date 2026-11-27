package com.aistudio.classroomlms.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Note: Since the actual table discipline_records was created when bumping to version 7,
            // we write the table creation here. If it was already using destructive migration, 
            // the table might exist for some users or not exist for others updating from older versions.
            // Using CREATE TABLE IF NOT EXISTS makes it safer.
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `discipline_records` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `studentId` INTEGER NOT NULL, 
                    `classroomId` INTEGER NOT NULL, 
                    `date` TEXT NOT NULL, 
                    `timestamp` INTEGER NOT NULL, 
                    `category` TEXT NOT NULL, 
                    `severity` TEXT NOT NULL, 
                    `title` TEXT NOT NULL, 
                    `description` TEXT NOT NULL, 
                    `actionTaken` TEXT NOT NULL, 
                    `parentNotified` INTEGER NOT NULL, 
                    `resolved` INTEGER NOT NULL, 
                    FOREIGN KEY(`studentId`) REFERENCES `students`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`classroomId`) REFERENCES `classrooms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_discipline_records_studentId` ON `discipline_records` (`studentId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_discipline_records_classroomId` ON `discipline_records` (`classroomId`)")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `live_assessments` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `studentId` INTEGER NOT NULL, 
                    `classroomId` INTEGER NOT NULL, 
                    `date` TEXT NOT NULL, 
                    `timestamp` INTEGER NOT NULL, 
                    `taskType` TEXT NOT NULL, 
                    `topic` TEXT NOT NULL, 
                    `masteryLevel` TEXT NOT NULL, 
                    `diagnosticTags` TEXT NOT NULL, 
                    `remarks` TEXT NOT NULL, 
                    `photoEvidencePath` TEXT, 
                    `includeInCasGrade` INTEGER NOT NULL, 
                    FOREIGN KEY(`studentId`) REFERENCES `students`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`classroomId`) REFERENCES `classrooms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_live_assessments_studentId` ON `live_assessments` (`studentId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_live_assessments_classroomId` ON `live_assessments` (`classroomId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_live_assessments_date` ON `live_assessments` (`date`)")
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `exams` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `classroomId` INTEGER NOT NULL, 
                    `category` TEXT NOT NULL, 
                    `title` TEXT NOT NULL, 
                    `topicOrChapter` TEXT NOT NULL, 
                    `fullMarks` REAL NOT NULL, 
                    `passMarks` REAL NOT NULL, 
                    `date` TEXT NOT NULL, 
                    `createdAt` INTEGER NOT NULL, 
                    FOREIGN KEY(`classroomId`) REFERENCES `classrooms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_exams_classroomId` ON `exams` (`classroomId`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `exam_marks` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `examId` INTEGER NOT NULL, 
                    `studentId` INTEGER NOT NULL, 
                    `marksObtained` REAL, 
                    `remarks` TEXT NOT NULL, 
                    `isAbsent` INTEGER NOT NULL, 
                    `updatedAt` INTEGER NOT NULL, 
                    FOREIGN KEY(`examId`) REFERENCES `exams`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                    FOREIGN KEY(`studentId`) REFERENCES `students`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_exam_marks_examId` ON `exam_marks` (`examId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_exam_marks_studentId` ON `exam_marks` (`studentId`)")
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `classrooms` ADD COLUMN `isArchived` INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `lesson_plans` ADD COLUMN `unitNumber` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `lesson_plans` ADD COLUMN `allocatedHours` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `lesson_plans` ADD COLUMN `unitLearningOutcomes` TEXT NOT NULL DEFAULT ''")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `lesson_plan_sessions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `lessonPlanId` INTEGER NOT NULL,
                    `sessionOrder` INTEGER NOT NULL,
                    `learningOutcome` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `lecturerActivity` TEXT NOT NULL,
                    `studentActivity` TEXT NOT NULL,
                    `teachingAids` TEXT NOT NULL,
                    `evaluationType` TEXT NOT NULL,
                    `evaluationNote` TEXT NOT NULL,
                    `linkedAssignmentId` INTEGER,
                    `hours` REAL NOT NULL,
                    `isClosingSession` INTEGER NOT NULL,
                    FOREIGN KEY(`lessonPlanId`) REFERENCES `lesson_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_lesson_plan_sessions_lessonPlanId` ON `lesson_plan_sessions` (`lessonPlanId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_lesson_plan_sessions_linkedAssignmentId` ON `lesson_plan_sessions` (`linkedAssignmentId`)")
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `academic_events` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `startDate` TEXT NOT NULL,
                    `endDate` TEXT NOT NULL,
                    `notes` TEXT NOT NULL,
                    `academicYearLabel` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_events_startDate` ON `academic_events` (`startDate`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_events_category` ON `academic_events` (`category`)")
        }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `schools` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `institutionType` TEXT NOT NULL,
                    `address` TEXT NOT NULL,
                    `colorHex` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("ALTER TABLE `classrooms` ADD COLUMN `schoolId` INTEGER")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `classroom_activities` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `classroomId` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `date` TEXT NOT NULL,
                    `durationSeconds` INTEGER NOT NULL,
                    `firstWinnerStudentId` INTEGER,
                    `secondWinnerStudentId` INTEGER,
                    `thirdWinnerStudentId` INTEGER,
                    `notes` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    FOREIGN KEY(`classroomId`) REFERENCES `classrooms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_classroom_activities_classroomId` ON `classroom_activities` (`classroomId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_classroom_activities_date` ON `classroom_activities` (`date`)")
        }
    }
}
