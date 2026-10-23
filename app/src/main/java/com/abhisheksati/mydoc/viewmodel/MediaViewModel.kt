package com.abhisheksati.mydoc.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhisheksati.mydoc.domain.model.MediaFilter
import com.abhisheksati.mydoc.domain.model.MediaItem
import com.abhisheksati.mydoc.domain.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.launch
import java.util.Calendar

@Immutable
data class MediaGroup(
    val title: String,
    val items: List<MediaItem>
)

@Immutable
data class MediaUiState(
    val filter: MediaFilter = MediaFilter.ALL,
    val items: List<MediaItem> = emptyList(),
    val groups: List<MediaGroup> = emptyList(),
    val hasPermission: Boolean = false,
    val isPermissionRequested: Boolean = false,
    val isLoading: Boolean = false
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MediaViewModel(
    private val repository: MediaRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(MediaFilter.ALL)
    private val _hasPermission = MutableStateFlow(false)
    private val _isPermissionRequested = MutableStateFlow(false)
    private val _refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<MediaUiState> = combine(
        _filter,
        _hasPermission,
        _isPermissionRequested,
        _refreshTrigger
    ) { filter, hasPerm, isRequested, _ ->
        Triple(filter, hasPerm, isRequested)
    }.flatMapLatest { (filter, hasPerm, isRequested) ->
        if (!hasPerm) {
            MutableStateFlow(
                MediaUiState(
                    filter = filter,
                    hasPermission = false,
                    isPermissionRequested = isRequested,
                    isLoading = false
                )
            )
        } else {
            kotlinx.coroutines.flow.flow {
                emit(MediaUiState(filter = filter, hasPermission = true, isPermissionRequested = isRequested, isLoading = true))
                repository.getMedia(filter).collect { items ->
                    val groups = groupMediaByDate(items)
                    emit(
                        MediaUiState(
                            filter = filter,
                            items = items,
                            groups = groups,
                            hasPermission = true,
                            isPermissionRequested = isRequested,
                            isLoading = false
                        )
                    )
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MediaUiState()
    )

    fun checkPermission(context: Context) {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val img = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val vid = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            val partial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
            } else false
            (img && vid) || partial
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        _hasPermission.value = granted
    }

    fun setPermissionGranted(granted: Boolean) {
        _hasPermission.value = granted
        _isPermissionRequested.value = true
        if (granted) {
            refresh()
        }
    }

    fun setFilter(filter: MediaFilter) {
        _filter.value = filter
    }

    fun refresh() {
        _refreshTrigger.value = _refreshTrigger.value + 1
    }

    private fun groupMediaByDate(items: List<MediaItem>): List<MediaGroup> {
        if (items.isEmpty()) return emptyList()

        val now = Calendar.getInstance()
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterdayStart = todayStart - 86400000L
        val thisWeekStart = todayStart - (now.get(Calendar.DAY_OF_WEEK) - 1) * 86400000L
        val thisMonthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val groups = mutableMapOf<String, MutableList<MediaItem>>()

        for (item in items) {
            val millis = if (item.dateModifiedSec > 0) item.dateModifiedSec * 1000 else item.dateAddedSec * 1000
            val key = when {
                millis >= todayStart -> "TODAY"
                millis >= yesterdayStart -> "YESTERDAY"
                millis >= thisWeekStart -> "THIS WEEK"
                millis >= thisMonthStart -> "THIS MONTH"
                else -> "EARLIER"
            }
            groups.getOrPut(key) { mutableListOf() }.add(item)
        }

        // Preserve chronological order of keys
        val orderedKeys = listOf("TODAY", "YESTERDAY", "THIS WEEK", "THIS MONTH", "EARLIER")
        return orderedKeys.mapNotNull { key ->
            groups[key]?.let { list -> MediaGroup(key, list) }
        }
    }

    class Factory(
        private val repository: MediaRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MediaViewModel(repository) as T
        }
    }
}
