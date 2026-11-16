package com.abhisheksati.mydoc.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.abhisheksati.mydoc.MainActivity
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Home-screen "continue reading" widget. Rows carry a recents index, never a file path:
 *  the activity resolves the index against its own database. */
class ContinueReadingWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                val views = buildViews(context.applicationContext)
                appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, views) }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun buildViews(context: Context): RemoteViews {
        val recents = runCatching {
            AppDatabase.getInstance(context).recentDao().getAllRecents(ROW_IDS.size).first()
        }.getOrDefault(emptyList())
        return RemoteViews(context.packageName, R.layout.widget_continue_reading).apply {
            val emptyVisible = if (recents.isEmpty()) View.VISIBLE else View.GONE
            setViewVisibility(R.id.widget_empty, emptyVisible)
            // Tapping the frame's dead space opens the app; the rows carry their own links.
            setOnClickPendingIntent(R.id.widget_root, launchIntent(context))
            ROW_IDS.forEachIndexed { index, rowId ->
                val doc = recents.getOrNull(index)
                if (doc == null) {
                    setViewVisibility(rowId, View.GONE)
                    return@forEachIndexed
                }
                setViewVisibility(rowId, View.VISIBLE)
                setTextViewText(titleIds[index], doc.displayName)
                setTextViewText(
                    subtitleIds[index],
                    if (doc.pageCount > 0) {
                        context.getString(R.string.widget_page_progress, doc.lastPage + 1, doc.pageCount)
                    } else {
                        context.getString(R.string.widget_open_document)
                    }
                )
                setOnClickPendingIntent(rowId, rowIntent(context, index))
            }
        }
    }

    private fun launchIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
        return PendingIntent.getActivity(
            context,
            99,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun rowIntent(context: Context, index: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_OPEN_RECENT)
            .putExtra(MainActivity.EXTRA_RECENT_INDEX, index)
        return PendingIntent.getActivity(
            context,
            100 + index,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val ROW_IDS = intArrayOf(R.id.widget_row_0, R.id.widget_row_1)
        private val titleIds = intArrayOf(R.id.widget_row_0_title, R.id.widget_row_1_title)
        private val subtitleIds = intArrayOf(R.id.widget_row_0_subtitle, R.id.widget_row_1_subtitle)

        /** Repaint every placed widget after recents or reading progress change. */
        fun refresh(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = manager.getAppWidgetIds(ComponentName(appContext, ContinueReadingWidget::class.java))
            if (ids.isEmpty()) return
            scope.launch {
                val views = runCatching {
                    ContinueReadingWidget().buildViews(appContext)
                }.getOrNull() ?: return@launch
                ids.forEach { id -> manager.updateAppWidget(id, views) }
            }
        }
    }
}
