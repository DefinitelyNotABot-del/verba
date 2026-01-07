package com.imu.verba.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Information about a GitHub release.
 */
data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val publishedAt: String,
    val apkDownloadUrl: String?
)

/**
 * Manages checking for updates from GitHub releases.
 * Compares current app version with latest release tag.
 */
object UpdateManager {
    
    private const val GITHUB_OWNER = "DefinitelyNotABot-del"
    private const val GITHUB_REPO = "verba"
    private const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases"
    
    // Current app version - should match versionName in build.gradle.kts
    private const val CURRENT_VERSION = "1.0.0"
    
    /**
     * Check for updates from GitHub releases.
     * 
     * @return Latest release info if newer than current version, null if up to date
     */
    suspend fun checkForUpdates(): Result<ReleaseInfo?> = withContext(Dispatchers.IO) {
        try {
            val latestRelease = fetchLatestRelease()
            
            if (latestRelease != null && isNewerVersion(latestRelease.tagName)) {
                Result.success(latestRelease)
            } else {
                Result.success(null) // Up to date
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Fetch the latest release from GitHub API.
     */
    private fun fetchLatestRelease(): ReleaseInfo? {
        val url = URL(GITHUB_API_URL)
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "Verba-Android-App")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return null
            }
            
            val response = connection.inputStream.bufferedReader().readText()
            val releases = JSONArray(response)
            
            if (releases.length() == 0) {
                return null
            }
            
            // Get the first (latest) release
            val release = releases.getJSONObject(0)
            
            // Find APK asset URL if available
            var apkUrl: String? = null
            val assets = release.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.getString("name")
                    if (assetName.endsWith(".apk")) {
                        apkUrl = asset.getString("browser_download_url")
                        break
                    }
                }
            }
            
            return ReleaseInfo(
                tagName = release.getString("tag_name"),
                name = release.optString("name", release.getString("tag_name")),
                body = release.optString("body", ""),
                htmlUrl = release.getString("html_url"),
                publishedAt = release.getString("published_at"),
                apkDownloadUrl = apkUrl
            )
        } finally {
            connection.disconnect()
        }
    }
    
    /**
     * Compare version strings to check if new version is newer.
     * Handles formats like "v1.0.0", "1.0.0", "1.0"
     */
    private fun isNewerVersion(newVersionTag: String): Boolean {
        val currentParts = parseVersion(CURRENT_VERSION)
        val newParts = parseVersion(newVersionTag)
        
        for (i in 0 until maxOf(currentParts.size, newParts.size)) {
            val current = currentParts.getOrElse(i) { 0 }
            val new = newParts.getOrElse(i) { 0 }
            
            if (new > current) return true
            if (new < current) return false
        }
        
        return false // Same version
    }
    
    /**
     * Parse version string to list of integers.
     */
    private fun parseVersion(version: String): List<Int> {
        return version
            .removePrefix("v")
            .removePrefix("V")
            .split(".")
            .mapNotNull { it.toIntOrNull() }
    }
    
    /**
     * Get the current app version.
     */
    fun getCurrentVersion(): String = CURRENT_VERSION
    
    /**
     * Open the release page in browser.
     */
    fun openReleasePage(context: Context, releaseInfo: ReleaseInfo) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseInfo.htmlUrl))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
    
    /**
     * Open the APK download URL in browser.
     */
    fun downloadApk(context: Context, releaseInfo: ReleaseInfo) {
        val downloadUrl = releaseInfo.apkDownloadUrl ?: releaseInfo.htmlUrl
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}
