package com.abhisheksati.mydoc.rendering.engine

/**
 * Pure Kotlin Compound File Binary Format (CFBF / OLE2) parser.
 * Full support for FAT, chained DIFAT sectors (> 6.8MB files), directory streams,
 * and MiniFAT / MiniStream allocation tables.
 */
internal class Ole2File(private val data: ByteArray) {
    val sectorSize: Int
    private val fat: IntArray
    private val miniFat: IntArray
    private val miniStream: ByteArray
    private val miniSectorCutoff: Long
    private val dirEntries = mutableListOf<DirEntry>()

    data class DirEntry(
        val name: String,
        val type: Int,
        val startSector: Int,
        val size: Long
    )

    init {
        require(data.size >= 512) { "File too small for OLE2" }
        val sectorShift = getShortLE(data, 30).toInt()
        sectorSize = 1 shl (if (sectorShift in 9..12) sectorShift else 9)

        val numFatSectors = getIntLE(data, 44)
        val firstDirSector = getIntLE(data, 48)
        val cutoff = getIntLE(data, 56)
        miniSectorCutoff = if (cutoff > 0) cutoff.toLong() else 4096L
        val firstMiniFatSector = getIntLE(data, 60)
        val numMiniFatSectors = getIntLE(data, 64)
        val firstDifatSector = getIntLE(data, 68)
        val numDifatSectors = getIntLE(data, 72)

        // 1. Read first 109 DIFAT entries from header
        val fatSectorLocations = mutableListOf<Int>()
        for (i in 0 until 109) {
            val off = 76 + (i * 4)
            if (off + 4 <= 512) {
                val sec = getIntLE(data, off)
                if (sec >= 0) fatSectorLocations.add(sec)
            }
        }

        // 2. Traverse DIFAT sector chain if numDifatSectors > 0
        if (numDifatSectors > 0 && firstDifatSector >= 0) {
            var curDifatSec = firstDifatSector
            var difatCount = 0
            val difatEntriesPerSector = (sectorSize / 4) - 1
            val visitedDifat = HashSet<Int>()
            while (curDifatSec >= 0 && difatCount < numDifatSectors && visitedDifat.add(curDifatSec)) {
                val offset = (curDifatSec + 1) * sectorSize
                if (offset + sectorSize > data.size) break
                for (i in 0 until difatEntriesPerSector) {
                    val sec = getIntLE(data, offset + (i * 4))
                    if (sec >= 0) {
                        fatSectorLocations.add(sec)
                    }
                }
                curDifatSec = getIntLE(data, offset + difatEntriesPerSector * 4)
                difatCount++
            }
        }

        // 3. Assemble FAT table. The sector count is read straight from the header, so a few hundred
        // bytes of crafted file could ask for a gigabyte-scale array before anything is validated.
        // The container cannot hold more sectors than it has bytes, so the file size is the ceiling.
        val declaredFatInts = numFatSectors.coerceAtLeast(0).toLong() * (sectorSize / 4)
        val ceiling = data.size.toLong() + 1024L
        val listedFatInts = (fatSectorLocations.size.toLong() * (sectorSize / 4)).coerceAtMost(ceiling)
        val fatIntCount = maxOf(declaredFatInts.coerceAtMost(ceiling), listedFatInts, 1024L).toInt()
        fat = IntArray(fatIntCount)
        var fatIndex = 0
        for (fatSec in fatSectorLocations) {
            val secOffset = (fatSec + 1) * sectorSize
            if (secOffset + sectorSize <= data.size) {
                for (j in 0 until (sectorSize / 4)) {
                    if (fatIndex < fat.size) {
                        fat[fatIndex++] = getIntLE(data, secOffset + (j * 4))
                    }
                }
            }
        }

        // 4. Read Directory Entries
        val dirBytes = readSectorChain(firstDirSector, 2_000_000L)
        var dirOffset = 0
        while (dirOffset + 128 <= dirBytes.size) {
            val nameLen = getShortLE(dirBytes, dirOffset + 64).toInt()
            val type = dirBytes[dirOffset + 66].toInt() and 0xFF
            val startSec = getIntLE(dirBytes, dirOffset + 116)
            val streamSize = getLongLE(dirBytes, dirOffset + 120)

            val name = if (nameLen > 2) {
                String(dirBytes, dirOffset, minOf(nameLen - 2, 64), Charsets.UTF_16LE).trim()
            } else ""

            if (name.isNotEmpty()) {
                dirEntries.add(DirEntry(name, type, startSec, streamSize))
            }
            dirOffset += 128
        }

        // 5. Assemble MiniStream & MiniFAT
        val rootEntry = dirEntries.firstOrNull { it.type == 5 || it.name.equals("Root Entry", ignoreCase = true) }
        if (rootEntry != null && rootEntry.startSector >= 0 && rootEntry.size > 0) {
            miniStream = readSectorChain(rootEntry.startSector, rootEntry.size)
        } else {
            miniStream = ByteArray(0)
        }

        if (numMiniFatSectors > 0 && firstMiniFatSector >= 0) {
            val miniFatBytes = readSectorChain(firstMiniFatSector, numMiniFatSectors.toLong() * sectorSize)
            val miniFatInts = miniFatBytes.size / 4
            miniFat = IntArray(maxOf(miniFatInts, 64))
            for (i in 0 until miniFatInts) {
                miniFat[i] = getIntLE(miniFatBytes, i * 4)
            }
        } else {
            miniFat = IntArray(0)
        }
    }

    fun getStream(name: String): ByteArray? {
        val entry = dirEntries.find { it.name.equals(name, ignoreCase = true) } ?: return null
        if (entry.startSector < 0 || entry.size <= 0) return null
        if (entry.size < miniSectorCutoff && miniStream.isNotEmpty() && miniFat.isNotEmpty()) {
            return readMiniSectorChain(entry.startSector, entry.size)
        }
        return readSectorChain(entry.startSector, entry.size)
    }

    private fun readMiniSectorChain(startSector: Int, maxSize: Long): ByteArray {
        val miniSectorSize = 64
        val out = java.io.ByteArrayOutputStream()
        var curSector = startSector
        val clampedMax = minOf(maxSize, 5_000_000L)
        var remaining = clampedMax
        val visited = HashSet<Int>()

        while (curSector >= 0 && curSector < miniFat.size && remaining > 0) {
            if (!visited.add(curSector)) break
            val offset = curSector * miniSectorSize
            if (offset >= miniStream.size) break
            val toRead = minOf(miniSectorSize.toLong(), remaining).toInt()
            if (offset + toRead <= miniStream.size) {
                out.write(miniStream, offset, toRead)
                remaining -= toRead
            } else break

            val next = miniFat[curSector]
            if (next < 0 || next >= 0xFFFFFFFD.toInt() || next == curSector) break
            curSector = next
        }
        return out.toByteArray()
    }

    private fun readSectorChain(startSector: Int, maxSize: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        var curSector = startSector
        val clampedMax = minOf(maxSize, 50_000_000L)
        var remaining = clampedMax
        val visited = HashSet<Int>()

        while (curSector >= 0 && curSector < fat.size && remaining > 0) {
            if (!visited.add(curSector)) break // Cycle detected in FAT table
            val offset = (curSector + 1) * sectorSize
            if (offset >= data.size) break
            val toRead = minOf(sectorSize.toLong(), remaining).toInt()
            if (offset + toRead <= data.size) {
                out.write(data, offset, toRead)
                remaining -= toRead
            } else break

            val next = fat[curSector]
            if (next < 0 || next == curSector) break
            curSector = next
        }
        return out.toByteArray()
    }

    companion object {
        fun getShortLE(b: ByteArray, offset: Int): Short {
            if (offset + 1 >= b.size) return 0
            return ((b[offset].toInt() and 0xFF) or ((b[offset + 1].toInt() and 0xFF) shl 8)).toShort()
        }

        fun getIntLE(b: ByteArray, offset: Int): Int {
            if (offset + 3 >= b.size) return 0
            return (b[offset].toInt() and 0xFF) or
                ((b[offset + 1].toInt() and 0xFF) shl 8) or
                ((b[offset + 2].toInt() and 0xFF) shl 16) or
                ((b[offset + 3].toInt() and 0xFF) shl 24)
        }

        fun getLongLE(b: ByteArray, offset: Int): Long {
            if (offset + 7 >= b.size) return 0L
            val low = getIntLE(b, offset).toLong() and 0xFFFFFFFFL
            val high = getIntLE(b, offset + 4).toLong() and 0xFFFFFFFFL
            return low or (high shl 32)
        }
    }
}
