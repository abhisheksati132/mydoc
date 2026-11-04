package com.abhisheksati.mydoc.rendering.engine

import java.security.MessageDigest

/**
 * Keys for cached Office-to-PDF conversions.
 *
 * Converters previously keyed on `ByteArray.contentHashCode()`, a 32-bit value, alongside the
 * file length. Two different documents of the same size could therefore be handed the same cached
 * PDF — silently showing one file's content for another. A truncated SHA-256 makes that
 * unreachable in practice, and hashing bytes we already hold costs far less than a wrong render.
 */
object ConversionKey {

    fun of(bytes: ByteArray): String = hex(MessageDigest.getInstance("SHA-256").digest(bytes), 16)

    /** For sources that are streamed rather than held in memory: the identity of the document. */
    fun ofIdentity(vararg parts: String): String =
        hex(MessageDigest.getInstance("SHA-256").digest(parts.joinToString("\u0000").toByteArray()), 16)

    private fun hex(digest: ByteArray, take: Int): String {
        val sb = StringBuilder(take * 2)
        for (i in 0 until minOf(take, digest.size)) sb.append("%02x".format(digest[i].toInt() and 0xFF))
        return sb.toString()
    }
}
