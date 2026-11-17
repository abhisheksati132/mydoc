package com.abhisheksati.mydoc.tools.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

internal enum class TtsEngineEvent { START, DONE, STOP, ERROR }

/**
 * Platform-neutral handle onto a system TTS engine. Keeping the reading state machine behind
 * this interface lets pause/resume and stale-callback behaviour be unit tested without a
 * device engine; production uses [AndroidTtsSpeechEngine].
 */
internal interface TtsSpeechEngine {
    val label: String

    fun setSpeechRate(rate: Float)

    /** Returns false when the engine rejects the utterance. */
    fun speak(text: String, utteranceId: String): Boolean

    fun stop()

    fun release()
}

internal class AndroidTtsSpeechEngine(
    context: Context,
    private val onReady: (Boolean) -> Unit,
    private val onEvent: (TtsEngineEvent, String?) -> Unit
) : TtsSpeechEngine, TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var pendingRate = 1.0f
    @Volatile private var ready = false

    override var label: String = ""
        private set

    override fun onInit(status: Int) {
        val engine = tts
        if (status != TextToSpeech.SUCCESS || engine == null) {
            onReady(false)
            return
        }
        val defaultEngine = engine.defaultEngine
        val packageManager = appContext.packageManager
        label = ttsEngineDisplayName(defaultEngine) { pkg ->
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        }.orEmpty()
        val language = engine.setLanguage(Locale.getDefault())
        if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.language = Locale.US
        }
        engine.setSpeechRate(pendingRate)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = onEvent(TtsEngineEvent.START, utteranceId)

            override fun onDone(utteranceId: String?) = onEvent(TtsEngineEvent.DONE, utteranceId)

            override fun onStop(utteranceId: String?, interrupted: Boolean) =
                onEvent(TtsEngineEvent.STOP, utteranceId)

            @Deprecated("Superseded by onError(String, Int); abstract in the base class.")
            override fun onError(utteranceId: String?) = onEvent(TtsEngineEvent.ERROR, utteranceId)

            override fun onError(utteranceId: String, errorCode: Int) =
                onEvent(TtsEngineEvent.ERROR, utteranceId)
        })
        ready = true
        onReady(true)
    }

    override fun setSpeechRate(rate: Float) {
        pendingRate = rate
        if (ready) tts?.setSpeechRate(rate)
    }

    override fun speak(text: String, utteranceId: String): Boolean {
        if (!ready) return false
        return tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId) == TextToSpeech.SUCCESS
    }

    override fun stop() {
        tts?.stop()
    }

    override fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
