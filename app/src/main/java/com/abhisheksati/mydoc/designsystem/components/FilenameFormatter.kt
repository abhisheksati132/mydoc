package com.abhisheksati.mydoc.designsystem.components

/**
 * Truncates a filename in the middle so the extension stays visible, without ever returning more
 * characters than asked for — or fewer than zero.
 */
fun formatMiddleTruncated(name: String, maxChars: Int = 36): String {
    if (name.length <= maxChars) return name
    if (maxChars <= ELLIPSIS.length) return name.take(maxChars.coerceAtLeast(0))

    val dotIndex = name.lastIndexOf('.')
    val extensionLength = name.length - dotIndex - 1
    val hasUsableExtension = dotIndex > 0 && extensionLength in 1..MAX_PRESERVED_EXTENSION_CHARS

    val base = if (hasUsableExtension) name.substring(0, dotIndex) else name
    val extension = if (hasUsableExtension) name.substring(dotIndex) else ""
    val budget = maxChars - extension.length - ELLIPSIS.length

    if (budget < 2) {
        // A long extension or a small limit leaves nothing to show *of* the name. Keeping the head
        // is the honest degradation; the old arithmetic asked for a negative number of characters.
        return name.take((maxChars - ELLIPSIS.length).coerceAtLeast(0)) + ELLIPSIS
    }

    val front = (budget * 6 / 10).coerceIn(1, budget - 1)
    val back = budget - front
    return base.take(front) + ELLIPSIS + base.takeLast(back) + extension
}

private const val ELLIPSIS = "…"

/** Past this length an extension is treated as part of the name rather than sacred. */
private const val MAX_PRESERVED_EXTENSION_CHARS = 8
