package com.abhisheksati.mydoc.update

import java.net.URLDecoder
import java.util.Locale

/**
 * Resolves the latest release through GitHub's website routes. The REST API allows only
 * 60 unauthenticated requests per hour per IP; devices behind carrier-grade NAT can hit
 * HTTP 403 there while the website routes keep working. Only github.com URLs for this
 * repository and plain version tags are trusted.
 */
internal class GitHubWebReleaseClient(
    private val owner: String,
    private val repo: String,
    private val fetcher: HttpFetcher
) {

    internal data class WebRelease(
        val tagName: String,
        val assets: List<ReleaseAssetEntry>,
        val checksumUrls: Map<String, String>,
        val releaseUrl: String
    )

    internal data class ParsedAssets(
        val assets: List<ReleaseAssetEntry>,
        val checksumUrls: Map<String, String>
    )

    private val tagLocation = Regex(
        "^https://github\\.com/${Regex.escape(owner)}/${Regex.escape(repo)}/releases/tag/([^/?#]+)$"
    )

    fun fetchLatest(): WebRelease? {
        val tagName = resolveLatestTag() ?: return null
        val listing = fetcher.fetch(expandedAssetsUrl(tagName), "text/html")
        check(listing.code == 200) { "Release asset listing failed with HTTP ${listing.code}" }
        val parsed = parseExpandedAssets(listing.body, tagName, owner, repo)
        return WebRelease(tagName, parsed.assets, parsed.checksumUrls, releaseUrl(tagName))
    }

    private fun resolveLatestTag(): String? {
        val response = fetcher.fetch(latestUrl(), "text/html")
        if (response.code == 404) return null
        check(response.code in REDIRECT_CODES) { "Latest release lookup failed with HTTP ${response.code}" }
        val tag = tagFromLatestRedirect(response.location)
            ?: throw IllegalStateException("Latest release lookup returned an unusable redirect")
        check(GitHubUpdateService.isValidTag(tag)) { "Latest release tag is not a plain version tag" }
        return tag
    }

    private fun latestUrl() = "https://github.com/$owner/$repo/releases/latest"

    private fun expandedAssetsUrl(tag: String) = "https://github.com/$owner/$repo/releases/expanded_assets/$tag"

    private fun releaseUrl(tag: String) = "https://github.com/$owner/$repo/releases/tag/$tag"

    private fun tagFromLatestRedirect(location: String?): String? {
        val match = location?.let(tagLocation::find) ?: return null
        return try {
            URLDecoder.decode(match.groupValues[1], "UTF-8").takeIf(GitHubUpdateService::isValidTag)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    companion object {
        private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
        private val ROW = Regex("""<li\b[^>]*class="[^"]*Box-row[^"]*"[^>]*>([\s\S]*?)</li>""")
        private val HREF = Regex("""href="(/[^"]+)"""")
        private val SHA256_IN_ROW = Regex("""sha256:([a-fA-F0-9]{64})""")
        private val SIZE_IN_ROW = Regex(""">([0-9]+(?:\.[0-9]+)?)\s*(Bytes|KB|MB|GB)</span>""")

        fun parseExpandedAssets(html: String, tag: String, owner: String, repo: String): ParsedAssets {
            val downloadPrefix = "/$owner/$repo/releases/download/$tag/"
            val assets = mutableListOf<ReleaseAssetEntry>()
            val checksums = mutableMapOf<String, String>()

            ROW.findAll(html).forEach { row ->
                val rowHtml = row.groupValues[1]
                val href = HREF.find(rowHtml)?.groupValues?.get(1) ?: return@forEach
                if (!href.startsWith(downloadPrefix)) return@forEach
                val encodedName = href.removePrefix(downloadPrefix).substringBefore('?')
                if (encodedName.isBlank() || encodedName.contains('/')) return@forEach
                val name = try {
                    URLDecoder.decode(encodedName, "UTF-8")
                } catch (_: IllegalArgumentException) {
                    return@forEach
                }
                if (name.contains('/') || name.contains('\\')) return@forEach

                val url = "https://github.com$href"
                if (name.endsWith(".sha256", ignoreCase = true)) {
                    checksums[name.substring(0, name.length - ".sha256".length)] = url
                    return@forEach
                }

                val digest = SHA256_IN_ROW.find(rowHtml)?.groupValues?.get(1)?.lowercase(Locale.ROOT)
                val sizeBytes = SIZE_IN_ROW.find(rowHtml)?.let {
                    parseHumanSize(it.groupValues[1], it.groupValues[2])
                } ?: 0L
                assets.add(ReleaseAssetEntry(name, url, sizeBytes, digest))
            }

            return ParsedAssets(assets, checksums)
        }

        private fun parseHumanSize(value: String, unit: String): Long {
            val amount = value.toDoubleOrNull() ?: return 0L
            val multiplier = when (unit) {
                "Bytes" -> 1.0
                "KB" -> 1024.0
                "MB" -> 1024.0 * 1024.0
                "GB" -> 1024.0 * 1024.0 * 1024.0
                else -> return 0L
            }
            return (amount * multiplier).toLong()
        }
    }
}
