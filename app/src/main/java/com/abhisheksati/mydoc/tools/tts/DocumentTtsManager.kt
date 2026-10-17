package com.abhisheksati.mydoc.tools.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class TtsPlaybackState {
    IDLE,
    INITIALIZING,
    PLAYING,
    PAUSED,
    ERROR
}

class DocumentTtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _playbackState = MutableStateFlow(TtsPlaybackState.IDLE)
    val playbackState: StateFlow<TtsPlaybackState> = _playbackState.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _currentUtterance = MutableStateFlow("")
    val currentUtterance: StateFlow<String> = _currentUtterance.asStateFlow()

    private var fullText: String = ""
    private var chunks: List<String> = emptyList()
    private var currentChunkIndex = 0

    init {
        _playbackState.value = TtsPlaybackState.INITIALIZING
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.language = Locale.US
                }
                engine.setSpeechRate(_speechRate.value)
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _playbackState.value = TtsPlaybackState.PLAYING
                    }

                    override fun onDone(utteranceId: String?) {
                        currentChunkIndex++
                        if (currentChunkIndex < chunks.size) {
                            speakCurrentChunk()
                        } else {
                            _playbackState.value = TtsPlaybackState.IDLE
                            currentChunkIndex = 0
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        _playbackState.value = TtsPlaybackState.ERROR
                    }
                })
                isInitialized = true
                _playbackState.value = TtsPlaybackState.IDLE
            }
        } else {
            _playbackState.value = TtsPlaybackState.ERROR
        }
    }

    fun startReading(text: String) {
        if (text.isBlank()) return
        fullText = text
        // Break by sentences or chunks of ~250 words
        chunks = chunkText(text)
        currentChunkIndex = 0
        speakCurrentChunk()
    }

    private fun speakCurrentChunk() {
        if (!isInitialized || tts == null) return
        if (currentChunkIndex >= chunks.size) {
            _playbackState.value = TtsPlaybackState.IDLE
            return
        }

        val chunk = chunks[currentChunkIndex]
        _currentUtterance.value = chunk
        _playbackState.value = TtsPlaybackState.PLAYING

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "chunk_$currentChunkIndex")
        tts?.speak(chunk, TextToSpeech.QUEUE_FLUSH, params, "chunk_$currentChunkIndex")
    }

    fun pause() {
        if (_playbackState.value == TtsPlaybackState.PLAYING) {
            tts?.stop()
            _playbackState.value = TtsPlaybackState.PAUSED
        }
    }

    fun resume() {
        if (_playbackState.value == TtsPlaybackState.PAUSED) {
            speakCurrentChunk()
        }
    }

    fun stop() {
        tts?.stop()
        currentChunkIndex = 0
        _currentUtterance.value = ""
        _playbackState.value = TtsPlaybackState.IDLE
    }

    fun setSpeed(rate: Float) {
        _speechRate.value = rate
        tts?.setSpeechRate(rate)
    }

    private fun chunkText(text: String): List<String> {
        val result = mutableListOf<String>()
        val paragraphs = text.split(Regex("\n+"))
        for (para in paragraphs) {
            val trimmed = para.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.length < 500) {
                result.add(trimmed)
            } else {
                // Split long paragraphs by sentences
                val sentences = trimmed.split(Regex("(?<=[.!?])\\s+"))
                var currentBuffer = StringBuilder()
                for (sentence in sentences) {
                    if (currentBuffer.length + sentence.length > 500) {
                        result.add(currentBuffer.toString().trim())
                        currentBuffer = StringBuilder()
                    }
                    currentBuffer.append(sentence).append(" ")
                }
                if (currentBuffer.isNotBlank()) {
                    result.add(currentBuffer.toString().trim())
                }
            }
        }
        return if (result.isEmpty()) listOf(text) else result
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {}
    }
}
