package com.abhisheksati.mydoc.update

import com.abhisheksati.mydoc.domain.model.AppRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

class GitHubUpdateService(
    private val owner: String = "abhisheksati132",
    private val repo: String = "mydoc"
) {

    companion object {
        private val VALID_TAG_REGEX = Regex("""^v?\d+(\.\d+){1,3}$""")

        fun isValidTag(tag: String): Boolean = VALID_TAG_REGEX.matches(tag.trim())
    }

    suspend fun fetchLatestRelease(): Result<AppRelease?> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val url = URL(endpoint)
            var connection: HttpURLConnection? = null
            try {
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    instanceFollowRedirects = false
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "mydoc-android-updater")
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    // No releases have been published yet in this repository
                    return@runCatching null
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw IllegalStateException("GitHub API returned HTTP $responseCode")
                }

                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                parseReleaseJson(responseText)
            } finally {
                connection?.disconnect()
            }
        }
    }

    fun parseReleaseJson(jsonString: String): AppRelease? =
        parseReleaseJson(jsonString, android.os.Build.SUPPORTED_ABIS?.toList().orEmpty())

    internal fun parseReleaseJson(jsonString: String, supportedAbis: List<String>): AppRelease? {
        val root = JSONObject(jsonString)

        val isDraft = root.optBoolean("draft", false)
        val isPrerelease = root.optBoolean("prerelease", false)
        if (isDraft || isPrerelease) {
            return null
        }

        val tagName = root.optString("tag_name", "") ?: ""
        if (tagName.isBlank() || !isValidTag(tagName)) {
            return null
        }

        val versionName = tagName.removePrefix("v").removePrefix("V")
        val body = root.optString("body", "No release notes provided.") ?: "No release notes provided."
        val publishedAt = root.optString("published_at", "") ?: ""

        val assets = root.optJSONArray("assets") ?: return null
        val apkAssets = mutableListOf<Triple<String, String, Long>>()
        val checksumMap = mutableMapOf<String, String>()
        val apiAssets = mutableMapOf<String, Pair<String, String>>()

        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name", "") ?: ""
            val downloadUrl = asset.optString("browser_download_url", "") ?: ""
            val size = asset.optLong("size", 0)

            if (name.startsWith("mydoc-v") && name.endsWith(".apk", ignoreCase = true)) {
                apkAssets.add(Triple(name, downloadUrl, size))
                val id = asset.optLong("id", 0)
                val digest = asset.optString("digest", "").takeIf { it.startsWith("sha256:") }
                    ?.removePrefix("sha256:")
                if (id > 0 && digest != null && digest.matches(Regex("[a-fA-F0-9]{64}"))) {
                    apiAssets[name] = "https://api.github.com/repos/$owner/$repo/releases/assets/$id" to digest
                }
            } else if (name.endsWith(".sha256", ignoreCase = true)) {
                // Strict match: <apk name>.sha256 (case-insensitive stripping)
                val exactBaseApkName = name.substring(0, name.length - ".sha256".length)
                checksumMap[exactBaseApkName] = downloadUrl
            }
        }

        val baseName = "mydoc-v$versionName"
        val candidateNames = supportedAbis.map { "$baseName-$it.apk" } +
            listOf("$baseName-universal.apk", "$baseName.apk")
        val assetsByName = apkAssets.associateBy { it.first }
        val selectedApk = candidateNames.firstNotNullOfOrNull(assetsByName::get) ?: return null

        val apkName = selectedApk.first
        val apkUrl = selectedApk.second
        val apkSize = selectedApk.third

        // Strict: only match <apk name>.sha256 for the selected APK. No fallback to other APKs' checksums!
        val apiAsset = apiAssets[apkName]
        val checksumUrl = checksumMap[apkName]
        if (checksumUrl == null && apiAsset == null) throw IllegalStateException("Release asset $apkName does not have a matching checksum asset ($apkName.sha256)")

        return AppRelease(
            tagName = tagName,
            versionName = versionName,
            releaseNotes = body,
            apkDownloadUrl = apkUrl,
            apkSizeBytes = apkSize,
            checksumUrl = checksumUrl,
            publishedAt = publishedAt,
            assetApiUrl = apiAsset?.first,
            assetSha256 = apiAsset?.second
        )
    }
}
