package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VersionInfo(
    @field:Json(name = "latestVersionCode") val latestVersionCode: Int = 1,
    @field:Json(name = "latestVersionName") val latestVersionName: String = "1.0",
    @field:Json(name = "downloadUrl") val downloadUrl: String = "",
    @field:Json(name = "releaseNotes") val releaseNotes: String = ""
)
