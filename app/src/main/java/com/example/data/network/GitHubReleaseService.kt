package com.example.data.network

import com.example.data.model.VersionInfo
import retrofit2.http.GET
import retrofit2.http.Url

interface GitHubReleaseService {
    @GET
    suspend fun getVersionInfo(
        @Url url: String = VERSION_JSON_URL
    ): VersionInfo

    companion object {
        const val VERSION_JSON_URL = "https://gist.githubusercontent.com/premicist/c37235585c59b215e5b5ee147bd73c1f/raw/classroom_lms_version.json"
    }
}
