package com.abhisheksati.mydoc.tools.scan.cropper

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test

class BitmapRotationPixelsTest {
    @Test fun rectangularQuarterTurnsRetainEveryPixelAndLeaveTheSourceUntouched() {
        val pixels = intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt(),
            0xFFFFFF00.toInt(), 0xFF00FFFF.toInt(), 0xFFFF00FF.toInt())
        val source = Bitmap.createBitmap(pixels, 2, 3, Bitmap.Config.ARGB_8888)
        try {
            for ((degrees, order) in listOf(90 to listOf(4,2,0,5,3,1), 270 to listOf(1,3,5,0,2,4),
                -90 to listOf(1,3,5,0,2,4), 450 to listOf(4,2,0,5,3,1))) {
                val rotated = BitmapRotation.rotate(source, degrees)
                try {
                    assertEquals(3, rotated.width)
                    assertEquals(2, rotated.height)
                    val actual = IntArray(6)
                    rotated.getPixels(actual, 0, 3, 0, 0, 3, 2)
                    assertArrayEquals("Rotation $degrees", order.map { pixels[it] }.toIntArray(), actual)
                } finally { rotated.recycle() }
            }
            val original = IntArray(6)
            source.getPixels(original, 0, 2, 0, 0, 2, 3)
            assertArrayEquals(pixels, original)
        } finally { source.recycle() }
    }
}
