package com.abhisheksati.mydoc.update

import com.abhisheksati.mydoc.domain.model.AppRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection

internal data class ReleaseAssetEntry(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long = 0,
    val digestSha256: String? = null
)

class GitHubUpdateService internal constructor(
    private val owner: String = "abhisheksati132",
    private val repo: String = "mydoc",
    private val fetcher: HttpFetcher = HttpUrlConnectionFetcher()
) {

    companion object {
        private val VALID_TAG_REGEX = Regex("""^v?\d+(\.\d+){1,3}$""")

        internal const val RATE_LIMIT_MESSAGE =
            "GitHub is temporarily limiting update checks. Please try again in a few minutes."

        fun isValidTag(tag: String): Boolean = VALID_TAG_REGEX.matches(tag.trim())
    }

    private val webClient = GitHubWebReleaseClient(owner, repo, fetcher)

    suspend fun fetchLatestRelease(): Result<AppRelease?> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val response = fetcher.fetch(endpoint, "application/vnd.github+json")
            when (response.code) {
                HttpURLConnection.HTTP_OK -> parseReleaseJson(response.body)
                // No releases have been published yet in this repository
                HttpURLConnection.HTTP_NOT_FOUND -> null
                HttpURLConnection.HTTP_FORBIDDEN, 429 -> fetchLatestReleaseViaWeb()
                else -> throw IllegalStateException("GitHub API returned HTTP ${response.code}")
            }
        }
    }

    private fun fetchLatestReleaseViaWeb(): AppRelease? {
        val web = try {
            webClient.fetchLatest()
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            throw IllegalStateException(RATE_LIMIT_MESSAGE)
        } ?: return null
        return buildRelease(
            tagName = web.tagName,
            body = "",
            publishedAt = "",
            apkAssets = web.assets,
            checksumUrls = web.checksumUrls,
            supportedAbis = defaultSupportedAbis()
        )
    }

    fun parseReleaseJson(jsonString: String): AppRelease? =
        parseReleaseJson(jsonString, defaultSupportedAbis())

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

        val body = root.optString("body", "No release notes provided.") ?: "No release notes provided."
        val publishedAt = root.optString("published_at", "") ?: ""

        val assets = root.optJSONArray("assets") ?: return null
        val apkAssets = mutableListOf<ReleaseAssetEntry>()
        val checksumUrls = mutableMapOf<String, String>()
        val apiAssetUrls = mutableMapOf<String, String>()

        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name", "") ?: ""
            val downloadUrl = asset.optString("browser_download_url", "") ?: ""
            val size = asset.optLong("size", 0)

            if (name.startsWith("mydoc-v") && name.endsWith(".apk", ignoreCase = true)) {
                val digest = asset.optString("digest", "")
                    .takeIf { it.startsWith("sha256:") }
                    ?.removePrefix("sha256:")
                    ?.takeIf { it.matches(Regex("[a-fA-F0-9]{64}")) }
                apkAssets.add(ReleaseAssetEntry(name, downloadUrl, size, digest))
                val id = asset.optLong("id", 0)
                if (id > 0 && digest != null) {
                    apiAssetUrls[name] = "https://api.github.com/repos/$owner/$repo/releases/assets/$id"
                }
            } else if (name.endsWith(".sha256", ignoreCase = true)) {
                // Strict match: <apk name>.sha256 (case-insensitive stripping)
                val exactBaseApkName = name.substring(0, name.length - ".sha256".length)
                checksumUrls[exactBaseApkName] = downloadUrl
            }
        }

        return buildRelease(tagName, body, publishedAt, apkAssets, checksumUrls, supportedAbis, apiAssetUrls)
    }

    internal fun buildRelease(
        tagName: String,
        body: String,
        publishedAt: String,
        apkAssets: List<ReleaseAssetEntry>,
        checksumUrls: Map<String, String>,
        supportedAbis: List<String>,
        apiAssetUrls: Map<String, String> = emptyMap()
    ): AppRelease? {
        if (!isValidTag(tagName)) return null
        val versionName = tagName.removePrefix("v").removePrefix("V")

        val baseName = "mydoc-v$versionName"
        val candidateNames = supportedAbis.map { "$baseName-$it.apk" } +
            listOf("$baseName-universal.apk", "$baseName.apk")
        val assetsByName = apkAssets.associateBy { it.name }
        val selectedApk = candidateNames.firstNotNullOfOrNull(assetsByName::get) ?: return null

        // Strict: only match <apk name>.sha256 for the selected APK. No fallback to other APKs' checksums!
        val checksumUrl = checksumUrls[selectedApk.name]
        if (checksumUrl == null && selectedApk.digestSha256 == null) {
            throw IllegalStateException(
                "Release asset ${selectedApk.name} does not have a matching checksum asset (${selectedApk.name}.sha256)"
            )
        }

        return AppRelease(
            tagName = tagName,
            versionName = versionName,
            releaseNotes = body,
            apkDownloadUrl = selectedApk.downloadUrl,
            apkSizeBytes = selectedApk.sizeBytes,
            checksumUrl = checksumUrl,
            publishedAt = publishedAt,
            assetApiUrl = apiAssetUrls[selectedApk.name],
            assetSha256 = selectedApk.digestSha256
        )
    }

    private fun defaultSupportedAbis(): List<String> =
        android.os.Build.SUPPORTED_ABIS?.toList().orEmpty()
}
