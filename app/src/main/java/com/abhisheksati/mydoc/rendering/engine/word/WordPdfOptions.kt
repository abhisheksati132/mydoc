package com.abhisheksati.mydoc.rendering.engine.word

/** Escape every password code unit so LOK's legacy comma-delimited options cannot interpret it. */
internal fun wordPdfOptions(password: String?): String {
    val common = "\"UseLosslessCompression\":{\"type\":\"boolean\",\"value\":\"true\"}," +
        "\"ReduceImageResolution\":{\"type\":\"boolean\",\"value\":\"false\"}," +
        "\"UseTaggedPDF\":{\"type\":\"boolean\",\"value\":\"true\"}," +
        "\"ExportBookmarks\":{\"type\":\"boolean\",\"value\":\"true\"}"
    if (password == null) return "{$common}"
    val encoded = password.map { "\\u" + it.code.toString(16).padStart(4, '0') }.joinToString("")
    return "{$common,\"EncryptFile\":{\"type\":\"boolean\",\"value\":\"true\"}," +
        "\"DocumentOpenPassword\":{\"type\":\"string\",\"value\":\"$encoded\"}}"
}
