package com.aistudio.classroomlms.ui.util

import com.aistudio.classroomlms.data.entity.StudentEntity
import java.math.BigInteger

enum class StudentSortOrder(val label: String) {
    NAME("Name (A-Z)"),
    ROLL_NUMBER("Roll number")
}

fun <T> List<T>.sortedByStudent(
    order: StudentSortOrder,
    studentOf: (T) -> StudentEntity
): List<T> = sortedWith { left, right ->
    val leftStudent = studentOf(left)
    val rightStudent = studentOf(right)
    val primaryComparison = when (order) {
        StudentSortOrder.NAME -> compareNaturally(leftStudent.name, rightStudent.name)
        StudentSortOrder.ROLL_NUMBER -> compareRollNumbers(
            leftStudent.studentNumber,
            rightStudent.studentNumber
        )
    }

    if (primaryComparison != 0) {
        primaryComparison
    } else {
        val nameComparison = compareNaturally(leftStudent.name, rightStudent.name)
        if (nameComparison != 0) nameComparison else leftStudent.id.compareTo(rightStudent.id)
    }
}

private fun compareRollNumbers(left: String, right: String): Int {
    if (left.isBlank() != right.isBlank()) return if (left.isBlank()) 1 else -1
    return compareNaturally(left, right)
}

private val naturalNumberParts = Regex("\\d+|\\D+")

private fun compareNaturally(left: String, right: String): Int {
    val leftParts = naturalNumberParts.findAll(left).map { it.value }.toList()
    val rightParts = naturalNumberParts.findAll(right).map { it.value }.toList()

    for (index in 0 until minOf(leftParts.size, rightParts.size)) {
        val leftPart = leftParts[index]
        val rightPart = rightParts[index]
        val comparison = if (leftPart.first() in '0'..'9' && rightPart.first() in '0'..'9') {
            BigInteger(leftPart).compareTo(BigInteger(rightPart))
        } else {
            leftPart.compareTo(rightPart, ignoreCase = true)
        }
        if (comparison != 0) return comparison
    }

    return leftParts.size.compareTo(rightParts.size)
}
