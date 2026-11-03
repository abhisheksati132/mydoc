package com.abhisheksati.mydoc.designsystem.components

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The app's only transient-message channel. Toast was used in 54 places and could not
 * take the theme, an action, or a dismiss, so results read as system noise.
 */
object AppMessages {

    data class Message(val id: Long, val text: String, val duration: SnackbarDuration)

    private var nextId = 0L

    private val _latest = MutableStateFlow<Message?>(null)
    val latest: StateFlow<Message?> = _latest.asStateFlow()

    fun show(text: String, long: Boolean = false) {
        _latest.value = Message(
            id = nextId++,
            text = text,
            duration = if (long) SnackbarDuration.Long else SnackbarDuration.Short
        )
    }

    fun consume(id: Long) {
        if (_latest.value?.id == id) _latest.value = null
    }
}

/** Mechanical replacement for `Toast.makeText(context, text, duration).show()`. */
fun Context.showMessage(text: String, long: Boolean = false) = AppMessages.show(text, long)

@Composable
fun AppSnackbarHost(modifier: Modifier = Modifier) {
    val hostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        AppMessages.latest.collect { message ->
            if (message != null) {
                hostState.showSnackbar(
                    message = message.text,
                    duration = message.duration,
                    withDismissAction = true
                )
                AppMessages.consume(message.id)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        SnackbarHost(
            hostState = hostState,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(
                    start = AppSpacing.medium,
                    end = AppSpacing.medium,
                    top = AppSpacing.medium,
                    bottom = AppSpacing.navBarHeight + AppSpacing.medium
                )
        )
    }
}
