package com.abhisheksati.mydoc.domain.repository

import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.FileSortOrder
import com.abhisheksati.mydoc.domain.model.FolderProperties
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.domain.model.StorageVolume
import java.io.File

interface StorageRepository {
    fun getStorageVolumes(): List<StorageVolume>
    suspend fun listDirectory(directory: File, showHidden: Boolean, sortOrder: FileSortOrder): List<StorageItem>
    suspend fun createDirectory(parent: File, name: String): Result<File>
    suspend fun createFile(parent: File, name: String): Result<File>
    suspend fun rename(source: File, newName: String): Result<File>
    suspend fun delete(files: List<File>): Result<Int>
    suspend fun copy(sources: List<File>, targetDir: File, onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }): Result<Int>
    suspend fun move(sources: List<File>, targetDir: File, onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }): Result<Int>
    suspend fun getFolderProperties(directory: File): FolderProperties
    suspend fun calculateHash(file: File, algorithm: String = "SHA-256"): String
    suspend fun zip(sources: List<File>, destinationZip: File, onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }): Result<File>
    suspend fun unzip(zipFile: File, destinationDir: File, onProgress: (extractedFiles: Int) -> Unit = {}): Result<File>
    suspend fun queryCategory(category: FileCategory): List<StorageItem>
    suspend fun searchFiles(rootDirectory: File, query: String, recursive: Boolean = false): List<StorageItem>
    fun getTrashItems(): kotlinx.coroutines.flow.Flow<List<com.abhisheksati.mydoc.domain.model.TrashItem>>
    suspend fun moveToTrash(files: List<File>): Result<List<com.abhisheksati.mydoc.domain.model.TrashItem>>
    suspend fun restoreFromTrash(items: List<com.abhisheksati.mydoc.domain.model.TrashItem>): Result<Int>
    suspend fun deletePermanently(items: List<com.abhisheksati.mydoc.domain.model.TrashItem>): Result<Int>
    suspend fun emptyTrash(): Result<Int>
}

