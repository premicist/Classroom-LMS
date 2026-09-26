package com.example.data.repository

import com.example.BuildConfig
import com.example.data.model.GitHubRelease
import com.example.data.network.GitHubReleaseService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val isUpdateAvailable: Boolean,
    val currentVersionName: String,
    val latestVersionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val publishedAt: String = ""
)

class UpdateRepository(
    private val owner: String = "premicist",
    private val repo: String = "Classroom-LMS"
) {
    private val gitHubService: GitHubReleaseService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubReleaseService::class.java)
    }

    suspend fun checkLatestRelease(
        currentVersion: String = BuildConfig.VERSION_NAME
    ): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val release = gitHubService.getLatestRelease(owner, repo)
            val latestVersionTag = release.tagName.trim()
            val cleanLatestVersion = latestVersionTag.trimStart('v', 'V')
            val cleanCurrentVersion = currentVersion.trim().trimStart('v', 'V')

            val hasNewerVersion = isVersionHigher(cleanLatestVersion, cleanCurrentVersion)

            // Find an APK asset if uploaded with the release, otherwise use GitHub release page
            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            val downloadUrl = apkAsset?.browserDownloadUrl ?: release.htmlUrl

            val updateInfo = AppUpdateInfo(
                isUpdateAvailable = hasNewerVersion,
                currentVersionName = currentVersion,
                latestVersionName = latestVersionTag,
                releaseTitle = release.name ?: latestVersionTag,
                releaseNotes = release.body ?: "No release notes provided.",
                downloadUrl = downloadUrl,
                publishedAt = release.publishedAt ?: ""
            )

            Result.success(updateInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun isVersionHigher(latest: String, current: String): Boolean {
            val latestParts = latest.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }
            val currentParts = current.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }

            val maxLength = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLength) {
                val latestNum = latestParts.getOrElse(i) { 0 }
                val currentNum = currentParts.getOrElse(i) { 0 }
                if (latestNum > currentNum) return true
                if (latestNum < currentNum) return false
            }
            return false
        }
    }
}
