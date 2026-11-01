package com.abhisheksati.mydoc.designsystem.components

/**
 * Truncates a filename in the middle so the extension always stays visible.
 */
fun formatMiddleTruncated(name: String, maxChars: Int = 36): String {
    if (name.length <= maxChars) return name
    val dotIndex = name.lastIndexOf('.')
    if (dotIndex <= 0) {
        val half = (maxChars - 1) / 2
        return name.take(half) + "…" + name.takeLast(half)
    }
    val ext = name.substring(dotIndex)
    val base = name.substring(0, dotIndex)
    val availableForBase = maxChars - ext.length - 1
    if (availableForBase <= 4) return name.take(maxChars - ext.length - 1) + "…" + ext

    val frontChars = (availableForBase * 0.6).toInt()
    val backChars = availableForBase - frontChars
    return base.take(frontChars) + "…" + base.takeLast(backChars) + ext
}
