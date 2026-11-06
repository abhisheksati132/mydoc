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

                    @Deprecated("Superseded by onError(String, Int); abstract in the base class.")
                    override fun onError(utteranceId: String?) {
                        _playbackState.value = TtsPlaybackState.ERROR
                    }

                    override fun onError(utteranceId: String, errorCode: Int) {
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
        val queued = tts?.speak(chunk, TextToSpeech.QUEUE_FLUSH, params, "chunk_$currentChunkIndex")
        // The engine answers ERROR synchronously for a chunk it refuses (too long, language gone);
        // without this check the UI stayed on PLAYING with nothing being spoken.
        if (queued != TextToSpeech.SUCCESS) {
            _playbackState.value = TtsPlaybackState.ERROR
        }
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
        val limit = 500
        val result = mutableListOf<String>()
        fun flushChunk(buffer: StringBuilder) {
            val chunk = buffer.toString().trim()
            if (chunk.isNotEmpty()) result.add(chunk)
            buffer.setLength(0)
        }
        for (para in text.split(Regex("\n+"))) {
            val trimmed = para.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.length <= limit) {
                result.add(trimmed)
                continue
            }
            // Split long paragraphs by sentences. A sentence that is itself longer than the limit
            // (a URL, a CJK run with no spaces) is cut at a space near the limit rather than handed
            // to the engine whole, where it would be rejected or read in one unbounded utterance.
            val buffer = StringBuilder()
            for (sentence in trimmed.split(Regex("(?<=[.!?])\\s+"))) {
                var piece = sentence
                while (piece.length > limit) {
                    flushChunk(buffer)
                    val space = piece.lastIndexOf(' ', limit)
                    val cut = if (space > limit / 2) space else limit
                    result.add(piece.substring(0, cut).trim())
                    piece = piece.substring(cut).trimStart()
                }
                if (piece.isEmpty()) continue
                if (buffer.length + piece.length + 1 > limit) flushChunk(buffer)
                buffer.append(piece).append(' ')
            }
            flushChunk(buffer)
        }
        return result.ifEmpty { listOf(text.trim()) }.filter { it.isNotEmpty() }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {}
    }
}
