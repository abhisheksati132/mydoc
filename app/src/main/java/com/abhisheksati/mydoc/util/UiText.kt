package com.abhisheksati.mydoc.util

import android.content.Context
import androidx.annotation.StringRes

/**
 * Text that has not been resolved yet.
 *
 * A ViewModel has no reliable configuration, so it must not turn a resource into a sentence: a
 * locale or font change would leave the old wording on screen. Carrying the reference until a UI
 * boundary resolves it keeps every message localisable without threading a Context through the
 * whole state layer.
 */
sealed interface UiText {
    fun resolve(context: Context): String

    /** A resource plus its format arguments. Nested [UiText] arguments are resolved first. */
    data class Res(@StringRes val resId: Int, val args: List<Any> = emptyList()) : UiText {
        override fun resolve(context: Context): String =
            context.getString(
                resId,
                *args.map { if (it is UiText) it.resolve(context) else it }.toTypedArray()
            )
    }

    /** Copy the app authored itself (a typed load failure), which has no resource of its own. */
    data class Raw(val text: String) : UiText {
        override fun resolve(context: Context): String = text
    }

    /** Several sentences that belong together, joined by a space. */
    data class Joined(val parts: List<UiText>) : UiText {
        override fun resolve(context: Context): String =
            parts.joinToString(" ") { it.resolve(context) }
    }
}
