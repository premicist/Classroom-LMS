package com.example.data.database

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
}
