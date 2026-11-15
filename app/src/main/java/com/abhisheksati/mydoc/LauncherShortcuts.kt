package com.abhisheksati.mydoc

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/** Long-press launcher shortcuts. Published dynamically so they track the real package id
 *  (debug builds carry a suffix that static shortcut XML cannot resolve). */
object LauncherShortcuts {
    fun publish(context: Context) {
        val shortcuts = listOf(
            shortcut(
                context,
                id = "scan",
                shortLabel = context.getString(R.string.shortcut_scan_short),
                longLabel = context.getString(R.string.shortcut_scan_long),
                icon = R.drawable.ic_tile_scanner,
                action = MainActivity.ACTION_QUICK_SCAN
            ),
            shortcut(
                context,
                id = "search",
                shortLabel = context.getString(R.string.shortcut_search_short),
                longLabel = context.getString(R.string.shortcut_search_long),
                icon = R.drawable.ic_shortcut_search,
                action = MainActivity.ACTION_SEARCH
            ),
            shortcut(
                context,
                id = "merge",
                shortLabel = context.getString(R.string.shortcut_merge_short),
                longLabel = context.getString(R.string.shortcut_merge_long),
                icon = R.drawable.ic_shortcut_merge,
                action = MainActivity.ACTION_MERGE_PDF
            )
        )
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
    }

    private fun shortcut(
        context: Context,
        id: String,
        shortLabel: String,
        longLabel: String,
        icon: Int,
        action: String
    ): ShortcutInfoCompat = ShortcutInfoCompat.Builder(context, id)
        .setShortLabel(shortLabel)
        .setLongLabel(longLabel)
        .setIcon(IconCompat.createWithResource(context, icon))
        .setIntent(Intent(context, MainActivity::class.java).setAction(action))
        .build()
}
