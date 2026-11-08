package com.abhisheksati.mydoc.rendering.engine.word

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** Bounds-first decoding keeps a highly compressed image from allocating its full pixel size. */
internal object DocxImageDecoder {
    fun decode(bytes: ByteArray?): Bitmap? {
        if (bytes == null || bytes.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth.toLong() / sample * (bounds.outHeight / sample) > 4_000_000L ||
            bounds.outWidth / sample > 4096 || bounds.outHeight / sample > 4096) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
