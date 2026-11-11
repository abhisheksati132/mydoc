package com.abhisheksati.mydoc.util

import java.io.File
import java.security.MessageDigest

/**
 * Identity of document content, for every cache that stores a copy of it.
 *
 * Three caches key on documents: Office-to-PDF conversions, the offline `saved_docs` backup, and
 * staged thumbnail PDFs. All three used `hashCode()` or `uri.toString().hashCode().toString(16)`
 * — a 32-bit value, so one document could be handed another document's cached pages or thumbnail.
 * A truncated SHA-256 makes that unreachable in practice, and hashing costs far less than showing
 * the wrong file.
 *
 * A display name is never part of a URI's identity: renaming a file must not orphan its own
 * backup. Where a name is mixed in deliberately (a conversion cache for a stream with no stable
 * URI), the caller says so by passing both parts.
 */
object DocumentIdentity {

    /** Digest of content already held in memory. */
    fun ofBytes(bytes: ByteArray): String = hex(bytes)

    /** Digest of a stable identity string, e.g. a full URI. */
    fun ofText(vararg parts: String): String = hex(parts.joinToString(SEPARATOR).toByteArray())

    /** The `saved_docs` file name a document's offline copy takes. */
    fun savedDocsName(uri: String, displayName: String?): String =
        "${ofText(uri)}_${sanitizeDisplayName(displayName)}"

    /** True when [file] is a non-empty, committed offline copy of [uri]. Staging and metadata files are never valid copies. */
    fun isSavedCopyOf(file: File, uri: String): Boolean {
        val name = file.name
        if (name.endsWith(".tmp") || name.contains(".tmp") || name.endsWith(".meta") || name.endsWith(".part")) {
            return false
        }
        return name.startsWith("${ofText(uri)}_") && file.length() > 0L
    }

    fun sanitizeDisplayName(displayName: String?): String =
        (displayName ?: "document").replace(UNSAFE_NAME, "_")

    /** NUL separates parts so ("a", "bc") and ("ab", "c") cannot share a digest. */
    private const val SEPARATOR = "\u0000"

    private val UNSAFE_NAME = Regex("[^a-zA-Z0-9._-]")

    private const val HEX_LENGTH = 16

    private fun hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val sb = StringBuilder(HEX_LENGTH * 2)
        for (i in 0 until minOf(HEX_LENGTH, digest.size)) {
            sb.append("%02x".format(digest[i].toInt() and 0xFF))
        }
        return sb.toString()
    }
}
