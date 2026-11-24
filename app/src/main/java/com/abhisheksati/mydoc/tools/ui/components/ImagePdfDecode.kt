package com.abhisheksati.mydoc.tools.ui.components

/** Power-of-two sampling shared by editor previews, thumbnails, crop and export. */
internal fun editorImageSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    require(width > 0 && height > 0 && maxDimension > 0)
    var sample = 1
    // Long arithmetic avoids overflow from hostile image headers. Keep sampling until
    // the decoded longest edge fits, rather than stopping at an arbitrary 64x ceiling.
    while (sample < (1 shl 30) && maxOf(width, height).toLong() > maxDimension.toLong() * sample) {
        sample *= 2
    }
    return sample
}
