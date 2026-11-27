package com.abhisheksati.mydoc.data.archive

import android.system.OsConstants
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.IOException
import java.nio.file.FileAlreadyExistsException

/**
 * Kernel-enforced no-replace publication. Callers pin canonical parent paths
 * before an operation; resolving them again here could follow a swapped link.
 */
internal object ArchiveNativeFiles {
    init { System.loadLibrary("mydocarchiveio") }
    private external fun nativeMove(source: ByteArray, destination: ByteArray): Int
    private external fun nativeCreate(path: ByteArray, directory: Boolean): Int
    private external fun nativeDelete(path: ByteArray, directory: Boolean): Int

    fun delete(file: File, directory: Boolean = false) {
        val error = nativeDelete(file.absolutePath.toByteArray(Charsets.UTF_8), directory)
        if (error != 0 && error != OsConstants.ENOENT) throw IOException("Safe archive cleanup failed ($error)")
    }

    /** Parent identity is checked after opening it, including intermediate links. */
    fun output(file: File): java.io.OutputStream {
        val fd = nativeCreate(file.absolutePath.toByteArray(Charsets.UTF_8), false)
        if (fd < 0) throw IOException("Safe archive output creation failed (${-fd})")
        return ParcelFileDescriptor.AutoCloseOutputStream(ParcelFileDescriptor.adoptFd(fd))
    }

    fun directory(file: File) {
        val error = nativeCreate(file.absolutePath.toByteArray(Charsets.UTF_8), true)
        if (error != 0) throw IOException("Safe archive directory creation failed (${-error})")
    }

    fun move(source: File, destination: File) {
        val from = source.absoluteFile
        val to = destination.absoluteFile
        when (val error = nativeMove(from.path.toByteArray(Charsets.UTF_8), to.path.toByteArray(Charsets.UTF_8))) {
            0 -> Unit
            OsConstants.EEXIST -> throw FileAlreadyExistsException(destination.path)
            OsConstants.ENOSYS, OsConstants.EINVAL, OsConstants.EOPNOTSUPP -> throw IOException("This storage does not support safe archive publication")
            else -> throw IOException("Archive publication failed ($error)")
        }
    }
}
