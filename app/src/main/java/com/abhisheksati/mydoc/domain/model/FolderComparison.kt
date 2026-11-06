package com.abhisheksati.mydoc.domain.model

enum class ComparisonStatus { MATCHING, CHANGED, LEFT_ONLY, RIGHT_ONLY, UNKNOWN }
data class ComparisonEntry(val path: String, val status: ComparisonStatus, val directory: Boolean)
data class FolderComparison(val entries: List<ComparisonEntry>, val incomplete: Boolean)
