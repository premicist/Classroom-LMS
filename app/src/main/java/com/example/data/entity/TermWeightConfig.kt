package com.example.data.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TermWeightConfig(
    val term1Weight: Double = 25.0,
    val term2Weight: Double = 25.0,
    val finalExamWeight: Double = 50.0,
    val isEnabled: Boolean = false
) {
    fun isValid(): Boolean = (term1Weight + term2Weight + finalExamWeight) == 100.0
}
