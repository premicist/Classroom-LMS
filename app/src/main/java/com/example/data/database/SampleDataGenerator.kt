package com.example.data.database

import com.example.data.entity.AssignmentEntity
import com.example.data.entity.AssignmentType
import com.example.data.entity.AttendanceRecordEntity
import com.example.data.entity.AttendanceStatus
import com.example.data.entity.ClassroomEntity
import com.example.data.entity.HomeworkRecordEntity
import com.example.data.entity.HomeworkStatus
import com.example.data.entity.InterventionEntity
import com.example.data.entity.InterventionType
import com.example.data.entity.StudentEntity
import com.example.data.entity.SubmissionEntity
import com.example.data.entity.SubmissionStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object SampleDataGenerator {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun getPastDate(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return dateFormat.format(cal.time)
    }

    private fun getFutureDate(daysAhead: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, daysAhead)
        return dateFormat.format(cal.time)
    }

    suspend fun populateSampleData(database: AppDatabase) {
        val classroomDao = database.classroomDao()
        val studentDao = database.studentDao()
        val assignmentDao = database.assignmentDao()
        val submissionDao = database.submissionDao()
        val homeworkDao = database.homeworkRecordDao()
        val attendanceDao = database.attendanceDao()
        val interventionDao = database.interventionDao()

        // 1. Classrooms
        val mathClass = ClassroomEntity(
            name = "Algebra II - Honors",
            subject = "Mathematics",
            gradeLevel = "10th Grade",
            roomNumber = "Room 302",
            scheduleInfo = "Mon, Wed, Fri • 08:30 - 09:45 AM",
            colorHex = 0xFF6750A4, // Vibrant Purple
            iconName = "MATH",
            academicYear = "2025-2026"
        )
        val biologyClass = ClassroomEntity(
            name = "AP Biology",
            subject = "Science",
            gradeLevel = "11th Grade",
            roomNumber = "Lab 105",
            scheduleInfo = "Tue, Thu • 10:00 - 11:30 AM",
            colorHex = 0xFF00639B, // Vibrant Ocean
            iconName = "SCIENCE",
            academicYear = "2025-2026"
        )
        val historyClass = ClassroomEntity(
            name = "World History",
            subject = "Social Studies",
            gradeLevel = "10th Grade",
            roomNumber = "Room 204",
            scheduleInfo = "Mon, Wed, Fri • 01:15 - 02:30 PM",
            colorHex = 0xFF825500, // Vibrant Amber
            iconName = "HISTORY",
            academicYear = "2025-2026"
        )

        val mathId = classroomDao.insertClassroom(mathClass)
        val bioId = classroomDao.insertClassroom(biologyClass)
        val histId = classroomDao.insertClassroom(historyClass)

        // 2. Students for Math Class
        val mathStudents = listOf(
            StudentEntity(classroomId = mathId, name = "Alexander Hayes", studentNumber = "STU-1001", email = "a.hayes@school.edu", guardianContact = "555-0101", avatarColorHex = 0xFF6750A4, notes = "Excels in quadratic equations. Math olympiad team captain."),
            StudentEntity(classroomId = mathId, name = "Sophia Martinez", studentNumber = "STU-1002", email = "s.martinez@school.edu", guardianContact = "555-0102", avatarColorHex = 0xFF7D5260, notes = "Active participant, strong group leader."),
            StudentEntity(classroomId = mathId, name = "Lucas Chen", studentNumber = "STU-1003", email = "l.chen@school.edu", guardianContact = "555-0103", avatarColorHex = 0xFF00639B, notes = "Consistent top scorer in quizzes and proofs."),
            StudentEntity(classroomId = mathId, name = "Emma Watson", studentNumber = "STU-1004", email = "e.watson@school.edu", guardianContact = "555-0104", avatarColorHex = 0xFFB3261E, notes = "Needs targeted support with Rational Exponents and Factoring Trinomials."),
            StudentEntity(classroomId = mathId, name = "Ethan Miller", studentNumber = "STU-1005", email = "e.miller@school.edu", guardianContact = "555-0105", avatarColorHex = 0xFF825500, notes = "Working on homework submission timeliness and exam anxiety."),
            StudentEntity(classroomId = mathId, name = "Olivia Johnson", studentNumber = "STU-1006", email = "o.johnson@school.edu", guardianContact = "555-0106", avatarColorHex = 0xFF0284C7, notes = "Shows great improvement in graph analysis."),
            StudentEntity(classroomId = mathId, name = "Benjamin Davis", studentNumber = "STU-1007", email = "b.davis@school.edu", guardianContact = "555-0107", avatarColorHex = 0xFF4F46E5, notes = "Interschool robotics member, strong spatial reasoning."),
            StudentEntity(classroomId = mathId, name = "Ava Robinson", studentNumber = "STU-1008", email = "a.robinson@school.edu", guardianContact = "555-0108", avatarColorHex = 0xFFE11D48, notes = "100% homework streak, eligible for honors extension tasks.")
        )
        val mathStudentIds = studentDao.insertStudents(mathStudents)

        // Students for Biology Class
        val bioStudents = listOf(
            StudentEntity(classroomId = bioId, name = "Charlotte Taylor", studentNumber = "STU-2001", email = "c.taylor@school.edu", guardianContact = "555-0201", avatarColorHex = 0xFF059669, notes = "Excellent lab notebook organization."),
            StudentEntity(classroomId = bioId, name = "Daniel Kim", studentNumber = "STU-2002", email = "d.kim@school.edu", guardianContact = "555-0202", avatarColorHex = 0xFF2563EB, notes = "Strong grasp of cellular respiration."),
            StudentEntity(classroomId = bioId, name = "Mia Anderson", studentNumber = "STU-2003", email = "m.anderson@school.edu", guardianContact = "555-0203", avatarColorHex = 0xFF9333EA, notes = "Enthusiastic microscopy presenter."),
            StudentEntity(classroomId = bioId, name = "Noah Wilson", studentNumber = "STU-2004", email = "n.wilson@school.edu", guardianContact = "555-0204", avatarColorHex = 0xFFD97706, notes = "Needs make-up test for genetics unit.")
        )
        studentDao.insertStudents(bioStudents)

        // 3. Chronological Assignments for Math Class (Showing Performance Progression)
        val mathAssignments = listOf(
            AssignmentEntity(
                classroomId = mathId,
                title = "Unit 1: Linear Inequalities Review",
                description = "Foundational assessment on graphing two-variable inequalities and interval notation.",
                type = AssignmentType.HOMEWORK,
                maxPoints = 50.0,
                weightPercentage = 15.0,
                dueDate = getPastDate(24),
                assignedDate = getPastDate(28),
                isGraded = true
            ),
            AssignmentEntity(
                classroomId = mathId,
                title = "Unit 1 Quiz: Systems of Equations",
                description = "Quiz on substitution, elimination, and 3x3 matrices.",
                type = AssignmentType.QUIZ,
                maxPoints = 100.0,
                weightPercentage = 20.0,
                dueDate = getPastDate(18),
                assignedDate = getPastDate(21),
                isGraded = true
            ),
            AssignmentEntity(
                classroomId = mathId,
                title = "Polynomial Functions & Factoring Problem Set",
                description = "Complete exercises on grouping, synthetic division, and difference of cubes.",
                type = AssignmentType.HOMEWORK,
                maxPoints = 50.0,
                weightPercentage = 15.0,
                dueDate = getPastDate(12),
                assignedDate = getPastDate(16),
                isGraded = true
            ),
            AssignmentEntity(
                classroomId = mathId,
                title = "Unit 2 Assessment: Complex Numbers & Radicals",
                description = "Challenging unit covering imaginary numbers, radical conjugates, and rational exponents.",
                type = AssignmentType.EXAM,
                maxPoints = 100.0,
                weightPercentage = 25.0,
                dueDate = getPastDate(6),
                assignedDate = getPastDate(10),
                isGraded = true
            ),
            AssignmentEntity(
                classroomId = mathId,
                title = "Real-world Parabola Modeling Project",
                description = "Applied physics modeling project with quadratic curve fitting in Desmos and written report.",
                type = AssignmentType.PROJECT,
                maxPoints = 100.0,
                weightPercentage = 25.0,
                dueDate = getPastDate(1),
                assignedDate = getPastDate(8),
                isGraded = true
            ),
            AssignmentEntity(
                classroomId = mathId,
                title = "Midterm Examination",
                description = "Comprehensive midterm exam covering Units 1-4.",
                type = AssignmentType.EXAM,
                maxPoints = 150.0,
                weightPercentage = 30.0,
                dueDate = getFutureDate(12),
                assignedDate = getPastDate(2),
                isGraded = false
            )
        )
        val mathAssignmentIds = assignmentDao.insertAssignments(mathAssignments)

        // 4. Submissions for Math Class
        // Scores across assignments to show progression and a clear difficult assignment (Unit 2 Assessment):
        // Student: Alexander, Sophia, Lucas, Emma, Ethan, Olivia, Benjamin, Ava
        val a1Scores = listOf(48.0, 47.0, 50.0, 42.0, 44.0, 46.0, 49.0, 50.0) // Linear review (Easy, Avg ~93%)
        val a2Scores = listOf(96.0, 91.0, 98.0, 78.0, 83.0, 88.0, 94.0, 97.0) // Systems quiz (Moderate, Avg ~89%)
        val a3Scores = listOf(49.0, 48.0, 50.0, 36.0, 39.0, 45.0, 47.0, 50.0) // Factoring HW (Avg ~86%)
        // a4 is the DIFFICULT area across the class (Radicals & Complex numbers exam: Emma & Ethan struggled)
        val a4Scores = listOf(92.0, 84.0, 95.0, 58.0, 64.0, 76.0, 89.0, 94.0) // Difficult Exam (Avg ~81%, 2 failed/near fail)
        val a5Scores = listOf(98.0, 94.0, 97.0, 72.0, 78.0, 91.0, 96.0, 99.0) // Project (Avg ~90%)

        val allScores = listOf(a1Scores, a2Scores, a3Scores, a4Scores, a5Scores)

        for (aIdx in 0..4) {
            val aId = mathAssignmentIds[aIdx]
            val scoresList = allScores[aIdx]
            val subs = mathStudentIds.mapIndexed { sIdx, sId ->
                val score = scoresList[sIdx]
                val maxP = mathAssignments[aIdx].maxPoints
                SubmissionEntity(
                    assignmentId = aId,
                    studentId = sId,
                    classroomId = mathId,
                    status = SubmissionStatus.GRADED,
                    score = score,
                    submittedDate = "${getPastDate(mathAssignments[aIdx].dueDate.let { 5 })} 14:00",
                    feedback = if (score / maxP >= 0.9) "Outstanding work and meticulous mathematical rigor!" else if (score / maxP < 0.7) "Identified gaps in radical exponent rules. Recommend 1-on-1 tutoring review." else "Solid effort. Review errors in section 2.",
                    isChecked = true
                )
            }
            submissionDao.insertSubmissions(subs)
        }

        // Submissions for Midterm Exam (Pending / upcoming)
        val midtermId = mathAssignmentIds[5]
        val midtermSubs = mathStudentIds.map { sId ->
            SubmissionEntity(
                assignmentId = midtermId,
                studentId = sId,
                classroomId = mathId,
                status = SubmissionStatus.PENDING,
                score = null,
                submittedDate = null,
                feedback = "",
                isChecked = false
            )
        }
        submissionDao.insertSubmissions(midtermSubs)

        // 5. Homework Records for Math Class (past 6 school checks)
        val topics = listOf(
            "Sec 1.4 Linear Systems",
            "Sec 2.1 Factoring Trinomials",
            "Sec 2.3 Complex Conjugates",
            "Sec 2.5 Rational Exponents (Difficulty Spike)",
            "Sec 3.1 Quadratic Formula & Discriminant",
            "Sec 3.3 Parabola Transformations"
        )
        val hwRecords = mutableListOf<HomeworkRecordEntity>()
        topics.forEachIndexed { tIdx, topic ->
            val date = getPastDate(15 - tIdx * 2)
            mathStudentIds.forEachIndexed { sIdx, sId ->
                val status = when {
                    // Emma Watson struggles with rational exponents
                    sIdx == 3 && tIdx >= 3 -> if (tIdx == 3) HomeworkStatus.MISSING else HomeworkStatus.PARTIAL
                    // Ethan Miller has late/partial homework
                    sIdx == 4 && tIdx % 2 == 1 -> HomeworkStatus.PARTIAL
                    sIdx == 4 && tIdx == 4 -> HomeworkStatus.MISSING
                    sIdx == 7 -> HomeworkStatus.DONE // Ava perfect
                    (sIdx + tIdx) % 9 == 0 -> HomeworkStatus.PARTIAL
                    else -> HomeworkStatus.DONE
                }
                hwRecords.add(
                    HomeworkRecordEntity(
                        classroomId = mathId,
                        studentId = sId,
                        date = date,
                        topic = topic,
                        status = status,
                        notes = if (status == HomeworkStatus.PARTIAL) "Completed part A, needs guidance on application problems." else if (status == HomeworkStatus.MISSING) "No submission turned in by start of class." else ""
                    )
                )
            }
        }
        homeworkDao.insertHomeworkRecords(hwRecords)

        // 6. Attendance Records for Math Class (past 7 school days)
        val attendanceRecords = mutableListOf<AttendanceRecordEntity>()
        for (day in 0..6) {
            val date = getPastDate(day)
            mathStudentIds.forEachIndexed { sIdx, sId ->
                val status = when {
                    (sIdx == 3 && (day == 2 || day == 5)) -> AttendanceStatus.LATE
                    (sIdx == 4 && day == 3) -> AttendanceStatus.ABSENT
                    (sIdx == 2 && day == 6) -> AttendanceStatus.EXCUSED
                    else -> AttendanceStatus.PRESENT
                }
                attendanceRecords.add(
                    AttendanceRecordEntity(
                        classroomId = mathId,
                        studentId = sId,
                        date = date,
                        status = status,
                        remarks = if (status == AttendanceStatus.LATE) "Arrived 12 min late with pass" else if (status == AttendanceStatus.ABSENT) "Unexcused absence - guardian notified" else if (status == AttendanceStatus.EXCUSED) "Field trip excused" else ""
                    )
                )
            }
        }
        attendanceDao.insertAttendanceRecords(attendanceRecords)

        // 7. Teacher Interventions / Record Keeping Notes for Math Class
        val interventions = listOf(
            InterventionEntity(
                studentId = mathStudentIds[3], // Emma Watson (Needs Extra Support)
                classroomId = mathId,
                date = getPastDate(5),
                type = InterventionType.TUTORING,
                title = "1-on-1 Remediation: Radical & Rational Exponents",
                notes = "Reviewed radical conversions and fractional exponents. Assigned 5 practice exercises. Scheduled follow-up session for Thursday office hours.",
                resolved = false
            ),
            InterventionEntity(
                studentId = mathStudentIds[3], // Emma Watson
                classroomId = mathId,
                date = getPastDate(4),
                type = InterventionType.PARENT_CONTACT,
                title = "Guardian Check-in via Phone (Mrs. Watson)",
                notes = "Discussed recent Unit 2 exam score and shared supplemental study guide. Guardian agreed to monitor homework completion at home.",
                resolved = true
            ),
            InterventionEntity(
                studentId = mathStudentIds[4], // Ethan Miller (Needs Support with HW & Test Prep)
                classroomId = mathId,
                date = getPastDate(3),
                type = InterventionType.EXTRA_CREDIT,
                title = "Remedial Problem Set: Complex Numbers Recovery",
                notes = "Provided recovery packet covering complex conjugates for up to 10 points grade boost upon mastery demonstration.",
                resolved = false
            ),
            InterventionEntity(
                studentId = mathStudentIds[0], // Alexander Hayes (Enrichment / High Achiever)
                classroomId = mathId,
                date = getPastDate(2),
                type = InterventionType.ENRICHMENT,
                title = "Advanced Proofs & Math Olympiad Prep",
                notes = "Assigned supplementary college-level algebra proofs and invited to lead peer study group on quadratic modeling.",
                resolved = true
            ),
            InterventionEntity(
                studentId = mathStudentIds[7], // Ava Robinson (Enrichment)
                classroomId = mathId,
                date = getPastDate(1),
                type = InterventionType.ENRICHMENT,
                title = "Honors Extension: 3D Surface Generation Project",
                notes = "Ava completed standard parabola modeling early. Assigned 3D paraboloid modeling challenge in Desmos.",
                resolved = true
            )
        )
        interventionDao.insertInterventions(interventions)
    }
}
