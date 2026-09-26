package com.example.data.network

import com.example.data.model.GitHubRelease
import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubReleaseService {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String = "premicist",
        @Path("repo") repo: String = "Classroom-LMS"
    ): GitHubRelease
}
