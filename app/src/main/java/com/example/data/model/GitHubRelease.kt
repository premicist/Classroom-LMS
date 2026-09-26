package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubRelease(
    @field:Json(name = "tag_name") val tagName: String,
    @field:Json(name = "name") val name: String? = null,
    @field:Json(name = "body") val body: String? = null,
    @field:Json(name = "html_url") val htmlUrl: String,
    @field:Json(name = "published_at") val publishedAt: String? = null,
    @field:Json(name = "prerelease") val isPrerelease: Boolean = false,
    @field:Json(name = "draft") val isDraft: Boolean = false,
    @field:Json(name = "assets") val assets: List<GitHubAsset> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubAsset(
    @field:Json(name = "name") val name: String,
    @field:Json(name = "browser_download_url") val browserDownloadUrl: String,
    @field:Json(name = "content_type") val contentType: String? = null,
    @field:Json(name = "size") val size: Long = 0
)
