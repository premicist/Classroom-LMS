package com.aistudio.classroomlms.ui.util

import com.aistudio.classroomlms.data.entity.StudentEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class StudentSortingTest {
    @Test
    fun rollNumberSortUsesNaturalOrderAndPlacesMissingNumbersLast() {
        val students = listOf(
            student(id = 1, name = "Ten", rollNumber = "10"),
            student(id = 2, name = "Two", rollNumber = "2"),
            student(id = 3, name = "No number", rollNumber = ""),
            student(id = 4, name = "One", rollNumber = "1")
        )

        val sorted = students.sortedByStudent(StudentSortOrder.ROLL_NUMBER) { it }

        assertEquals(listOf("1", "2", "10", ""), sorted.map { it.studentNumber })
    }

    @Test
    fun nameSortUsesNaturalOrderWithoutChangingItems() {
        val students = listOf(
            student(id = 1, name = "Student 10", rollNumber = "2"),
            student(id = 2, name = "Student 2", rollNumber = "10"),
            student(id = 3, name = "student 1", rollNumber = "1")
        )

        val sorted = students.sortedByStudent(StudentSortOrder.NAME) { it }

        assertEquals(
            listOf("student 1", "Student 2", "Student 10"),
            sorted.map { it.name }
        )
    }

    private fun student(id: Long, name: String, rollNumber: String) = StudentEntity(
        id = id,
        classroomId = 1,
        name = name,
        studentNumber = rollNumber
    )
}
