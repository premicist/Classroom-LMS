package com.example.data.repository

import com.example.BuildConfig
import com.example.data.network.GitHubReleaseService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val isUpdateAvailable: Boolean,
    val currentVersionName: String,
    val latestVersionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val publishedAt: String = "",
    val sha256: String? = null
)

class UpdateRepository {
    private val gitHubService: GitHubReleaseService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
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
            .baseUrl("https://gist.githubusercontent.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubReleaseService::class.java)
    }

    suspend fun checkLatestRelease(
        currentVersion: String = BuildConfig.VERSION_NAME,
        currentVersionCode: Int = BuildConfig.VERSION_CODE
    ): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val versionInfo = gitHubService.getVersionInfo()
            val latestVersionTag = versionInfo.latestVersionName.trim()
            val cleanLatestVersion = latestVersionTag.trimStart('v', 'V')
            val cleanCurrentVersion = currentVersion.trim().trimStart('v', 'V')

            val hasNewerCode = versionInfo.latestVersionCode > currentVersionCode
            val hasNewerVersion = hasNewerCode || isVersionHigher(cleanLatestVersion, cleanCurrentVersion)

            val updateInfo = AppUpdateInfo(
                isUpdateAvailable = hasNewerVersion,
                currentVersionName = currentVersion,
                latestVersionName = latestVersionTag,
                releaseTitle = "Classroom LMS v$latestVersionTag",
                releaseNotes = versionInfo.releaseNotes.ifBlank { "New features and enhancements available." },
                downloadUrl = versionInfo.downloadUrl,
                sha256 = versionInfo.sha256
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

        fun verifyFileSha256(file: File, expectedSha256: String): Boolean {
            if (!file.exists() || !file.isFile || expectedSha256.isBlank()) return false
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                FileInputStream(file).use { fis ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                }
                val calculated = digest.digest().joinToString("") { "%02x".format(it) }
                calculated.equals(expectedSha256.trim(), ignoreCase = true)
            } catch (e: Exception) {
                false
            }
        }
    }
}
