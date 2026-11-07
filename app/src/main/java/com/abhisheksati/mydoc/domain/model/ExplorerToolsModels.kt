package com.abhisheksati.mydoc.domain.model

import java.io.File

data class ArchiveNode(val path: String, val name: String, val directory: Boolean, val bytes: Long)
data class ArchiveStamp(val bytes: Long, val modified: Long)
data class ArchiveIndex(val entries: List<ArchiveNode>, val fileCount: Int, val totalBytes: Long, val stamp: ArchiveStamp? = null) {
    fun children(folder: String): List<ArchiveNode> = entries.filter { node ->
        node.path != folder && node.path.removeSuffix("/").substringBeforeLast('/', "").let {
            (if (it.isEmpty()) "" else "$it/") == folder
        }
    }.sortedWith(compareBy<ArchiveNode> { !it.directory }.thenBy { it.name.lowercase(java.util.Locale.ROOT) })
}

data class FolderSizeEntry(val file: File, val bytes: Long)
data class FolderInsights(
    val totalBytes: Long,
    val fileCount: Int,
    val folderCount: Int,
    val folders: List<FolderSizeEntry>,
    val largestFiles: List<FolderSizeEntry>,
    val incomplete: Boolean
)

enum class TransferStatus { RUNNING, STOPPING, COMPLETED, PARTIAL, FAILED, CANCELLED }
data class TransferFailure(val name: String, val cause: Throwable?)
data class ExplorerTransfer(
    val id: Long,
    val operation: ClipboardOperation,
    val destination: File,
    val total: Int,
    val completed: Int = 0,
    val failed: Int = 0,
    val failures: List<TransferFailure> = emptyList(),
    val currentName: String = "",
    val status: TransferStatus = TransferStatus.RUNNING
) {
    val running: Boolean get() = status == TransferStatus.RUNNING || status == TransferStatus.STOPPING
}
data class ExplorerTransferReport(val completed: Int, val remaining: List<File>)
