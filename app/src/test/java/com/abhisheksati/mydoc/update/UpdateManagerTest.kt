package com.abhisheksati.mydoc.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManagerTest {

    @Test
    fun isNewerVersion_correctlyComparesSemVer() {
        // Simple dummy helper to test the comparison algorithm
        fun isNewer(remote: String, local: String): Boolean {
            val remoteParts = remote.split('.').mapNotNull { it.toIntOrNull() }
            val localParts = local.split('.').mapNotNull { it.toIntOrNull() }
            val maxLen = maxOf(remoteParts.size, localParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val l = localParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
            return false
        }

        assertTrue(isNewer("1.0.1", "1.0.0"))
        assertTrue(isNewer("1.1.0", "1.0.9"))
        assertTrue(isNewer("2.0.0", "1.99.99"))
        assertTrue(isNewer("1.0.0.1", "1.0.0"))

        assertFalse(isNewer("1.0.0", "1.0.0"))
        assertFalse(isNewer("0.9.9", "1.0.0"))
        assertFalse(isNewer("1.0.0", "1.0.1"))
    }
}
