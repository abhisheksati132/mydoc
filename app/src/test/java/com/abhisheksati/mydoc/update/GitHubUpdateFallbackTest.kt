package com.abhisheksati.mydoc.update

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression coverage for the HTTP 403 rate-limit failure of the in-app updater:
 * the REST API allows 60 unauthenticated requests per hour per IP, and devices behind
 * carrier-grade NAT routinely share that budget. The updater must fall back to the
 * website routes instead of surfacing "GitHub API returned HTTP 403".
 */
class GitHubUpdateFallbackTest {

    private class FakeFetcher(private val responses: Map<String, HttpResponse>) : HttpFetcher {
        val requested = mutableListOf<String>()
        override fun fetch(url: String, accept: String): HttpResponse {
            requested += url
            return responses[url] ?: HttpResponse(500)
        }
    }

    private val owner = "abhisheksati132"
    private val repo = "mydoc"
    private val tag = "v2.0.16"
    private val apiUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
    private val latestUrl = "https://github.com/$owner/$repo/releases/latest"
    private val assetsUrl = "https://github.com/$owner/$repo/releases/expanded_assets/$tag"
    private val appTagLocation = "https://github.com/$owner/$repo/releases/tag/$tag"

    private val arm64Digest = "d9b6590c6bad98852702dd2fb40d16e3181b426f9c493c1d8a48de36ba08ffbc"
    private val universalDigest = "a36a653d945873579c2cfbcfe9593e245658e2afa1b7f6f9dffeda2184cb50a6"

    private fun assetRow(name: String, size: String, digest: String? = null): String {
        val digestSpan = digest
            ?.let { """<span class="Truncate-text">sha256:$it</span>""" }
            .orEmpty()
        return """
            <li data-view-component="true" class="Box-row d-flex flex-column flex-md-row">
              <div><span>
                <a href="/$owner/$repo/releases/download/$tag/$name" rel="nofollow" data-turbo="false" class="wb-break-all">
                  <span class="text-bold">$name</span>
                </a>
              </span></div>
              <div>
                <span class="Truncate text-mono text-small color-fg-muted">$digestSpan</span>
                <span class="color-fg-muted text-right flex-shrink-0">$size</span>
              </div>
            </li>
        """.trimIndent()
    }

    private fun assetsHtml(includeChecksumAsset: Boolean = true): String = buildString {
        append("""<div class="Box Box--condensed tmp-mt-3"><ul>""")
        append(assetRow("mydoc-$tag-arm64-v8a.apk", "18.4 MB", arm64Digest))
        append(assetRow("mydoc-$tag-universal.apk", "45.8 MB", universalDigest))
        if (includeChecksumAsset) {
            append(assetRow("mydoc-$tag-universal.apk.sha256", "94 Bytes"))
        }
        append(assetRow("verification.txt", "2.1 KB"))
        append("""</ul></div>""")
    }

    private fun rateLimitedFetcher(
        latest: HttpResponse = HttpResponse(302, location = appTagLocation),
        assets: HttpResponse = HttpResponse(200, body = assetsHtml())
    ) = FakeFetcher(mapOf(apiUrl to HttpResponse(403), latestUrl to latest, assetsUrl to assets))

    @Test
    fun `rate limited API falls back to website routes`() {
        val fetcher = rateLimitedFetcher()
        val release = runBlocking { GitHubUpdateService(fetcher = fetcher).fetchLatestRelease().getOrThrow() }

        val resolved = checkNotNull(release)
        assertEquals(tag, resolved.tagName)
        assertEquals("2.0.16", resolved.versionName)
        assertEquals(
            "https://github.com/$owner/$repo/releases/download/$tag/mydoc-$tag-universal.apk",
            resolved.apkDownloadUrl
        )
        assertEquals(
            "https://github.com/$owner/$repo/releases/download/$tag/mydoc-$tag-universal.apk.sha256",
            resolved.checksumUrl
        )
        assertEquals(universalDigest, resolved.assetSha256)
        assertNull("website fallback has no API asset endpoint", resolved.assetApiUrl)
        assertEquals("only API, latest redirect and asset listing may be requested", listOf(apiUrl, latestUrl, assetsUrl), fetcher.requested)
    }

    @Test
    fun `healthy API is used without touching website routes`() {
        val fetcher = FakeFetcher(
            mapOf(
                apiUrl to HttpResponse(
                    200,
                    body = """
                        {"tag_name":"$tag","assets":[
                          {"name":"mydoc-$tag-universal.apk","browser_download_url":"https://github.com/$owner/$repo/releases/download/$tag/mydoc-$tag-universal.apk","size":48000000},
                          {"name":"mydoc-$tag-universal.apk.sha256","browser_download_url":"https://github.com/$owner/$repo/releases/download/$tag/mydoc-$tag-universal.apk.sha256","size":64}
                        ]}
                    """.trimIndent()
                )
            )
        )
        val release = runBlocking { GitHubUpdateService(fetcher = fetcher).fetchLatestRelease().getOrThrow() }

        assertNotNull(release)
        assertEquals(listOf(apiUrl), fetcher.requested)
    }

    @Test
    fun `fallback rejects redirects that leave this repository`() {
        val fetcher = rateLimitedFetcher(
            latest = HttpResponse(302, location = "https://evil.example.com/releases/tag/$tag")
        )
        val result = runBlocking { GitHubUpdateService(fetcher = fetcher).fetchLatestRelease() }

        assertTrue(result.isFailure)
        assertEquals(GitHubUpdateService.RATE_LIMIT_MESSAGE, result.exceptionOrNull()?.message)
        assertFalse("a foreign redirect must never be followed", assetsUrl in fetcher.requested)
    }

    @Test
    fun `fallback rejects redirects to a different repository`() {
        val fetcher = rateLimitedFetcher(
            latest = HttpResponse(302, location = "https://github.com/$owner/other/releases/tag/$tag")
        )
        val result = runBlocking { GitHubUpdateService(fetcher = fetcher).fetchLatestRelease() }

        assertTrue(result.isFailure)
        assertFalse(assetsUrl in fetcher.requested)
    }

    @Test
    fun `rate limit fallback failure reports a retry message, not a raw status`() {
        val fetcher = rateLimitedFetcher(assets = HttpResponse(500))
        val result = runBlocking { GitHubUpdateService(fetcher = fetcher).fetchLatestRelease() }

        assertEquals(GitHubUpdateService.RATE_LIMIT_MESSAGE, result.exceptionOrNull()?.message)
        assertFalse(result.exceptionOrNull()!!.message!!.contains("HTTP"))
    }

    @Test
    fun `html assets keep strict checksum matching and ABI preference`() {
        val parsed = GitHubWebReleaseClient.parseExpandedAssets(assetsHtml(), tag, owner, repo)
        val service = GitHubUpdateService(fetcher = FakeFetcher(emptyMap<String, HttpResponse>()))

        val arm64 = service.buildRelease(tag, "", "", parsed.assets, parsed.checksumUrls, listOf("arm64-v8a", "armeabi-v7a"))
        assertNotNull(arm64)
        assertEquals(
            "https://github.com/$owner/$repo/releases/download/$tag/mydoc-$tag-arm64-v8a.apk",
            arm64?.apkDownloadUrl
        )
        assertNull("the universal checksum must never be reused for the arm64 APK", arm64?.checksumUrl)
        assertEquals(arm64Digest, arm64?.assetSha256)
        assertNull(arm64?.assetApiUrl)

        val digestOnly = service.buildRelease(tag, "", "", parsed.assets, emptyMap<String, String>(), listOf("arm64-v8a"))
        assertNull("no sha256 asset means the release must lean on the server-side digest", digestOnly?.checksumUrl)
        assertEquals(arm64Digest, digestOnly?.assetSha256)
    }

    @Test
    fun `html assets without checksum data fail closed`() {
        val unverified = listOf(
            ReleaseAssetEntry(
                name = "mydoc-$tag-universal.apk",
                downloadUrl = "https://github.com/$owner/$repo/releases/download/$tag/mydoc-$tag-universal.apk",
                sizeBytes = 1
            )
        )
        val ex = assertThrows(IllegalStateException::class.java) {
            GitHubUpdateService(fetcher = FakeFetcher(emptyMap<String, HttpResponse>()))
                .buildRelease(tag, "", "", unverified, emptyMap<String, String>(), emptyList())
        }
        assertTrue(ex.message!!.contains("does not have a matching checksum asset"))
    }
}
