package com.abhisheksati.mydoc.tools.tts

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

enum class TtsPlaybackState {
    IDLE,
    INITIALIZING,
    PLAYING,
    PAUSED,
    ERROR
}

/**
 * The default engine may synthesize in the cloud, so the UI must name it rather than imply
 * on-device processing. Falls back to the package name when no readable label exists.
 */
internal fun ttsEngineDisplayName(defaultEnginePackage: String?, labelFor: (String) -> String?): String? {
    if (defaultEnginePackage.isNullOrBlank()) return null
    val label = runCatching { labelFor(defaultEnginePackage) }.getOrNull()
    return if (label.isNullOrBlank()) defaultEnginePackage else label
}

/**
 * Per-screen handle onto the shared [TtsPlaybackController]. Keeping this type means the
 * viewers did not change, while audio itself survives the viewer and is owned by
 * [TtsPlaybackService].
 */
class DocumentTtsManager(context: Context) {

    private val controller = TtsPlaybackController.get(context)

    val playbackState: StateFlow<TtsPlaybackState> = controller.playbackState
    val engineName: StateFlow<String> = controller.engineName
    val speechRate: StateFlow<Float> = controller.speechRate
    val currentUtterance: StateFlow<String> = controller.currentUtterance
    val currentPage: StateFlow<Int> = controller.currentPage
    val title: StateFlow<String> = controller.title

    fun startReading(text: String) = controller.startReading(text)

    fun startReadingPages(
        startPage: Int,
        pageCount: Int,
        title: String,
        loader: suspend (Int) -> String?
    ) = controller.startReadingPages(startPage, pageCount, title, loader)

    fun pause() = controller.pause()

    fun resume() = controller.resume()

    fun stop() = controller.stop()

    fun setSpeed(rate: Float) = controller.setSpeed(rate)

    /** Leaving the viewer ends playback; the engine itself stays warm for the next document. */
    fun shutdown() = controller.stop()
}
