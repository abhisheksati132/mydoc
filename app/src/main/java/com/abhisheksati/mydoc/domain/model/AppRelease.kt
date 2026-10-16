package com.abhisheksati.mydoc.domain.model

data class AppRelease(
    val tagName: String,
    val versionName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkSizeBytes: Long = 0,
    val checksumUrl: String? = null,
    val publishedAt: String = ""
)
