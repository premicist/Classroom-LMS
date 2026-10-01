package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClassroomLMSInstrumentedTest {

    private fun getDb() = com.example.data.database.AppDatabase.getInstance(
        InstrumentationRegistry.getInstrumentation().targetContext
    )

    private fun testClassroom(name: String = "Test Classroom") =
        com.example.data.entity.ClassroomEntity(
            name = name,
            subject = "Math",
            gradeLevel = "10th",
            roomNumber = "Room 101",
            scheduleInfo = "Mon-Wed-Fri"
        )

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.aistudio.classroomlms.kxpyv", appContext.packageName)
    }

    @Test
    fun databaseInitialization() {
        val db = getDb()
        assertNotNull("Database should be initialized", db)
    }

    @Test
    fun databaseDaosExist() {
        val db = getDb()
        assertNotNull(db.classroomDao())
        assertNotNull(db.studentDao())
        assertNotNull(db.assignmentDao())
        assertNotNull(db.attendanceDao())
        assertNotNull(db.submissionDao())
        assertNotNull(db.homeworkRecordDao())
        assertNotNull(db.interventionDao())
        assertNotNull(db.plannerDao())
        assertNotNull(db.classScheduleDao())
        assertNotNull(db.disciplineDao())
        assertNotNull(db.liveAssessmentDao())
    }

    // ── Classroom CRUD ────────────────────────────────────────

    @Test
    fun createClassroom() = runBlocking {
        val db = getDb()
        val id = db.classroomDao().insertClassroom(testClassroom("Create Test"))
        assertTrue("Classroom ID should be > 0", id > 0)
        db.classroomDao().deleteClassroomById(id)
    }

    @Test
    fun readClassroom() = runBlocking {
        val db = getDb()
        val id = db.classroomDao().insertClassroom(testClassroom("Read Test"))
        val retrieved = db.classroomDao().getClassroomByIdOnce(id)

        assertNotNull("Classroom should be retrieved", retrieved)
        assertEquals("Read Test", retrieved?.name)
        assertEquals("Math", retrieved?.subject)

        db.classroomDao().deleteClassroomById(id)
    }

    @Test
    fun updateClassroom() = runBlocking {
        val db = getDb()
        val original = testClassroom("Before Update")
        val id = db.classroomDao().insertClassroom(original)

        db.classroomDao().updateClassroom(original.copy(id = id, name = "After Update"))
        val retrieved = db.classroomDao().getClassroomByIdOnce(id)

        assertEquals("After Update", retrieved?.name)
        db.classroomDao().deleteClassroomById(id)
    }

    @Test
    fun deleteClassroom() = runBlocking {
        val db = getDb()
        val id = db.classroomDao().insertClassroom(testClassroom("Delete Me"))
        db.classroomDao().deleteClassroomById(id)

        val retrieved = db.classroomDao().getClassroomByIdOnce(id)
        assertNull("Classroom should be deleted", retrieved)
    }

    // ── Student CRUD ──────────────────────────────────────────

    @Test
    fun createAndReadStudent() = runBlocking {
        val db = getDb()
        val classId = db.classroomDao().insertClassroom(testClassroom("Student Class"))

        val student = com.example.data.entity.StudentEntity(
            classroomId = classId,
            name = "Jane Smith",
            studentNumber = "S002",
            email = "jane@example.com"
        )
        val studentId = db.studentDao().insertStudent(student)
        assertTrue("Student ID should be > 0", studentId > 0)

        val students = db.studentDao().getStudentsByClassroomOnce(classId)
        assertTrue("Should contain the student", students.any { it.id == studentId })
        assertEquals("Jane Smith", students.first { it.id == studentId }.name)

        db.classroomDao().deleteClassroomById(classId) // CASCADE deletes student
    }

    @Test
    fun deleteStudentById() = runBlocking {
        val db = getDb()
        val classId = db.classroomDao().insertClassroom(testClassroom("Del Student Class"))

        val studentId = db.studentDao().insertStudent(
            com.example.data.entity.StudentEntity(
                classroomId = classId, name = "Delete Me", studentNumber = "D001"
            )
        )
        db.studentDao().deleteStudentById(studentId)

        val remaining = db.studentDao().getStudentsByClassroomOnce(classId)
        assertTrue("Student should be deleted", remaining.none { it.id == studentId })

        db.classroomDao().deleteClassroomById(classId)
    }

    @Test
    fun cascadeDeleteRemovesStudents() = runBlocking {
        val db = getDb()
        val classId = db.classroomDao().insertClassroom(testClassroom("Cascade Class"))

        db.studentDao().insertStudent(
            com.example.data.entity.StudentEntity(
                classroomId = classId, name = "Cascade Student", studentNumber = "C001"
            )
        )
        db.classroomDao().deleteClassroomById(classId)

        val orphans = db.studentDao().getStudentsByClassroomOnce(classId)
        assertTrue("CASCADE should remove students", orphans.isEmpty())
    }

    // ── Data Integrity ────────────────────────────────────────

    @Test
    fun multipleClassroomsIsolation() = runBlocking {
        val db = getDb()
        val id1 = db.classroomDao().insertClassroom(testClassroom("Class A"))
        val id2 = db.classroomDao().insertClassroom(testClassroom("Class B"))

        db.studentDao().insertStudent(
            com.example.data.entity.StudentEntity(
                classroomId = id1, name = "Student A", studentNumber = "A1"
            )
        )
        db.studentDao().insertStudent(
            com.example.data.entity.StudentEntity(
                classroomId = id2, name = "Student B", studentNumber = "B1"
            )
        )

        val studentsA = db.studentDao().getStudentsByClassroomOnce(id1)
        val studentsB = db.studentDao().getStudentsByClassroomOnce(id2)

        assertEquals("Class A should have 1 student", 1, studentsA.size)
        assertEquals("Class B should have 1 student", 1, studentsB.size)
        assertEquals("Student A", studentsA[0].name)
        assertEquals("Student B", studentsB[0].name)

        db.classroomDao().deleteClassroomById(id1)
        db.classroomDao().deleteClassroomById(id2)
    }
}
