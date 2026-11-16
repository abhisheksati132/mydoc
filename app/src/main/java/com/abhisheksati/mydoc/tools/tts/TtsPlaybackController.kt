package com.abhisheksati.mydoc.tools.tts

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Single, process-wide read-aloud engine. Screens reach it through [DocumentTtsManager];
 * [TtsPlaybackService] keeps it audible with the screen off and exposes media controls.
 *
 * Pages are read one after another so long documents behave like an audiobook: the viewer
 * follows [currentPage], and the notification survives leaving the activity.
 */
class TtsPlaybackController private constructor(private val context: Context) : TextToSpeech.OnInitListener {

    private enum class Outcome { DONE, PAUSED, STOPPED, ERROR }

    private data class Request(
        val title: String,
        val singleText: String?,
        val pageCount: Int,
        val loader: (suspend (Int) -> String?)?
    )

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    @Volatile private var initialized = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _playbackState = MutableStateFlow(TtsPlaybackState.INITIALIZING)
    val playbackState: StateFlow<TtsPlaybackState> = _playbackState.asStateFlow()

    private val _engineName = MutableStateFlow("")
    val engineName: StateFlow<String> = _engineName.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _currentUtterance = MutableStateFlow("")
    val currentUtterance: StateFlow<String> = _currentUtterance.asStateFlow()

    private val _currentPage = MutableStateFlow(-1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private var request: Request? = null
    private var readingJob: Job? = null
    private var pendingStart: (() -> Unit)? = null
    private var currentUtteranceId: String? = null
    private var awaiting: CompletableDeferred<Outcome>? = null
    private var activePage = 0
    private var activeChunk = 0
    private var pausedPage = 0
    private var pausedChunk = 0

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            _playbackState.value = TtsPlaybackState.ERROR
            pendingStart = null
            return
        }
        val engine = tts
        if (engine == null) {
            _playbackState.value = TtsPlaybackState.ERROR
            return
        }
        _engineName.value = ttsEngineDisplayName(engine.defaultEngine) { pkg ->
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.orEmpty()
        val result = engine.setLanguage(Locale.getDefault())
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.language = Locale.US
        }
        engine.setSpeechRate(_speechRate.value)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId == currentUtteranceId) {
                    _playbackState.value = TtsPlaybackState.PLAYING
                }
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == currentUtteranceId) complete(Outcome.DONE)
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                if (utteranceId == currentUtteranceId) complete(Outcome.PAUSED)
            }

            @Deprecated("Superseded by onError(String, Int); abstract in the base class.")
            override fun onError(utteranceId: String?) {
                complete(Outcome.ERROR)
            }

            override fun onError(utteranceId: String, errorCode: Int) {
                complete(Outcome.ERROR)
            }
        })
        initialized = true
        _playbackState.value = TtsPlaybackState.IDLE
        val start = pendingStart
        pendingStart = null
        start?.invoke()
    }

    private fun complete(outcome: Outcome) {
        val waiter = awaiting ?: return
        awaiting = null
        waiter.complete(outcome)
    }

    fun startReading(text: String, title: String? = null) {
        if (text.isBlank()) return
        val req = Request(title?.takeIf { it.isNotBlank() } ?: "Document", text, 1, null)
        begin(req, 0, 0)
    }

    /** Reads [startPage] through [pageCount] in order, pulling each page's text with [loader]. */
    fun startReadingPages(
        startPage: Int,
        pageCount: Int,
        title: String,
        loader: suspend (Int) -> String?
    ) {
        if (pageCount <= 0) return
        val req = Request(title.ifBlank { "Document" }, null, pageCount, loader)
        begin(req, startPage.coerceIn(0, pageCount - 1), 0)
    }

    private fun begin(req: Request, page: Int, chunk: Int) {
        request = req
        _title.value = req.title
        if (!initialized) {
            _playbackState.value = TtsPlaybackState.INITIALIZING
            pendingStart = { launchReading(req, page, chunk) }
            return
        }
        launchReading(req, page, chunk)
    }

    private fun launchReading(req: Request, startPage: Int, startChunk: Int) {
        readingJob?.cancel()
        awaiting?.cancel()
        awaiting = null
        currentUtteranceId = null
        val job = scope.launch {
            try {
                var page = startPage
                var chunk = startChunk
                while (page < req.pageCount) {
                    if (!isActive) return@launch
                    val text = req.singleText ?: withContext(Dispatchers.IO) {
                        runCatching { req.loader?.invoke(page) }.getOrNull()
                    }
                    if (text.isNullOrBlank()) {
                        page++
                        chunk = 0
                        continue
                    }
                    // Pause can land while a page's text is still loading; stop before speaking.
                    if (_playbackState.value == TtsPlaybackState.PAUSED) {
                        pausedPage = page
                        pausedChunk = chunk
                        return@launch
                    }
                    _currentPage.value = page
                    val chunks = chunkText(text)
                    if (chunk >= chunks.size) {
                        page++
                        chunk = 0
                        continue
                    }
                    while (chunk < chunks.size) {
                        if (!isActive) return@launch
                        activePage = page
                        activeChunk = chunk
                        _currentUtterance.value = chunks[chunk]
                        when (val outcome = speakAndAwait(chunks[chunk])) {
                            Outcome.DONE -> chunk++
                            Outcome.PAUSED -> {
                                pausedPage = page
                                pausedChunk = chunk
                                _playbackState.value = TtsPlaybackState.PAUSED
                                return@launch
                            }
                            Outcome.STOPPED -> return@launch
                            Outcome.ERROR -> {
                                _playbackState.value = TtsPlaybackState.ERROR
                                TtsPlaybackService.stopSession(context)
                                return@launch
                            }
                        }
                    }
                    page++
                    chunk = 0
                }
                _playbackState.value = TtsPlaybackState.IDLE
                _currentUtterance.value = ""
                TtsPlaybackService.stopSession(context)
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
        readingJob = job
        TtsPlaybackService.ensureStarted(context)
    }

    private suspend fun speakAndAwait(chunk: String): Outcome {
        if (!initialized) return Outcome.ERROR
        val engine = tts ?: return Outcome.ERROR
        val id = "tts_${System.nanoTime()}"
        currentUtteranceId = id
        val deferred = CompletableDeferred<Outcome>()
        awaiting = deferred
        _playbackState.value = TtsPlaybackState.PLAYING
        val params = Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id) }
        if (engine.speak(chunk, TextToSpeech.QUEUE_FLUSH, params, id) != TextToSpeech.SUCCESS) {
            awaiting = null
            return Outcome.ERROR
        }
        return deferred.await()
    }

    fun pause() {
        if (_playbackState.value != TtsPlaybackState.PLAYING) return
        _playbackState.value = TtsPlaybackState.PAUSED
        if (awaiting != null) {
            complete(Outcome.PAUSED)
        } else {
            pausedPage = activePage
            pausedChunk = activeChunk
        }
        tts?.stop()
    }

    fun resume() {
        if (_playbackState.value != TtsPlaybackState.PAUSED) return
        val req = request ?: return
        launchReading(req, pausedPage, pausedChunk)
    }

    fun stop() {
        val wasReading = request != null
        request = null
        pendingStart = null
        readingJob?.cancel()
        readingJob = null
        complete(Outcome.STOPPED)
        awaiting = null
        currentUtteranceId = null
        tts?.stop()
        _playbackState.value = TtsPlaybackState.IDLE
        _currentUtterance.value = ""
        _currentPage.value = -1
        if (wasReading) TtsPlaybackService.stopSession(context)
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

    companion object {
        // Holds the application context only, so the singleton cannot outlive the process.
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: TtsPlaybackController? = null

        fun get(context: Context): TtsPlaybackController = instance ?: synchronized(this) {
            instance ?: TtsPlaybackController(context.applicationContext).also { instance = it }
        }
    }
}
