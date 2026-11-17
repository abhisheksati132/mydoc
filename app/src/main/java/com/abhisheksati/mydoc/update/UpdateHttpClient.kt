package com.abhisheksati.mydoc.update

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

internal data class HttpResponse(
    val code: Int,
    val location: String? = null,
    val body: String = ""
)

internal fun interface HttpFetcher {
    fun fetch(url: String, accept: String): HttpResponse
}

internal class HttpUrlConnectionFetcher(
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 10_000
) : HttpFetcher {

    override fun fetch(url: String, accept: String): HttpResponse {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            instanceFollowRedirects = false
            setRequestProperty("Accept", accept)
            setRequestProperty("User-Agent", "mydoc-android-updater")
        }
        try {
            val code = connection.responseCode
            val location = connection.getHeaderField("Location")
            val body = if (code == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { it.readBounded() }
            } else {
                ""
            }
            return HttpResponse(code, location, body)
        } finally {
            connection.disconnect()
        }
    }

    private fun InputStream.readBounded(): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            out.write(buffer, 0, read)
            check(out.size() <= MAX_BODY_BYTES) { "Updater response exceeded the $MAX_BODY_BYTES byte limit" }
        }
        return out.toString(Charsets.UTF_8.name())
    }

    private companion object {
        const val MAX_BODY_BYTES = 2 * 1024 * 1024
    }
}
