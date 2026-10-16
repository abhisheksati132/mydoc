package com.abhisheksati.mydoc.domain.model

data class ViewState(
    val uri: String,
    val lastPage: Int = 0,
    val zoomScale: Float = 1.0f,
    val scrollOffsetX: Float = 0.0f,
    val scrollOffsetY: Float = 0.0f,
    val readingMode: ReadingMode = ReadingMode.NORMAL,
    val isContinuousScroll: Boolean = true,
    val updatedTimestamp: Long = System.currentTimeMillis()
)
