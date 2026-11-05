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
import kotlinx.coroutines.channels.Channel

/**
 * The app's only transient-message channel. Toast was used in 54 places and could not
 * take the theme, an action, or a dismiss, so results read as system noise.
 *
 * A queue, not a slot: the host is suspended inside `showSnackbar` while a message is on screen,
 * so a second message written to a single "latest" value replaced the first one silently — a
 * two-message result like "3 moved, 1 failed in the source location" could lose half of itself.
 */
object AppMessages {

    data class Message(val id: Long, val text: String, val duration: SnackbarDuration)

    private val nextId = java.util.concurrent.atomic.AtomicLong()
    private val queue = Channel<Message>(capacity = Channel.UNLIMITED)

    fun show(text: String, long: Boolean = false) {
        val message = Message(
            id = nextId.getAndIncrement(),
            text = text,
            duration = if (long) SnackbarDuration.Long else SnackbarDuration.Short
        )
        val delivered = queue.trySend(message).isSuccess
        if (!delivered) {
            android.util.Log.w("AppMessages", "message dropped: ${message.text}")
        }
    }

    /** The next message owed to the user, or null once the channel is closed. */
    suspend fun next(): Message? = queue.receiveCatching().getOrNull()
}

/** Mechanical replacement for `Toast.makeText(context, text, duration).show()`. */
fun Context.showMessage(text: String, long: Boolean = false) = AppMessages.show(text, long)

@Composable
fun AppSnackbarHost(modifier: Modifier = Modifier) {
    val hostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        while (true) {
            val message = AppMessages.next() ?: break
            hostState.showSnackbar(
                message = message.text,
                duration = message.duration,
                withDismissAction = true
            )
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
