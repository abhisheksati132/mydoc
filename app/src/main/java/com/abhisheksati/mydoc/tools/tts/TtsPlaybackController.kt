package com.abhisheksati.mydoc.tools.tts

import android.annotation.SuppressLint
import android.content.Context
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

/**
 * Single, process-wide read-aloud engine. Screens reach it through [DocumentTtsManager];
 * [TtsPlaybackService] keeps it audible with the screen off and exposes media controls.
 *
 * Pages are read one after another so long documents behave like an audiobook: the viewer
 * follows [currentPage]. Playback is viewer-scoped by design: the page loader borrows the
 * viewer's renderer and OCR engine, so leaving the document stops narration. The foreground
 * service keeps it audible with the screen off or the app in the background while it lasts.
 */
class TtsPlaybackController internal constructor(
    private val scope: CoroutineScope,
    private val startSession: () -> Unit,
    private val stopSession: () -> Unit,
    engineFactory: (onReady: (Boolean) -> Unit, onEvent: (TtsEngineEvent, String?) -> Unit) -> TtsSpeechEngine
) {

    private enum class Outcome { DONE, PAUSED, STOPPED, ERROR }

    private data class Request(
        val title: String,
        val singleText: String?,
        val pageCount: Int,
        val loader: (suspend (Int) -> String?)?
    )

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
    @Volatile private var currentUtteranceId: String? = null
    private var awaiting: CompletableDeferred<Outcome>? = null
    private var activePage = 0
    private var activeChunk = 0
    private var pausedPage = 0
    private var pausedChunk = 0
    @Volatile private var initialized = false

    private val engine: TtsSpeechEngine = engineFactory(::handleEngineReady, ::handleEngineEvent)

    private fun handleEngineReady(success: Boolean) {
        scope.launch {
            if (!success) {
                _playbackState.value = TtsPlaybackState.ERROR
                pendingStart = null
                return@launch
            }
            _engineName.value = engine.label
            initialized = true
            _playbackState.value = TtsPlaybackState.IDLE
            val start = pendingStart
            pendingStart = null
            start?.invoke()
        }
    }

    private fun handleEngineEvent(event: TtsEngineEvent, utteranceId: String?) {
        if (utteranceId == null) return
        // Engine callbacks arrive on a binder thread; apply them on the main scope and drop
        // anything that no longer belongs to the utterance being awaited.
        scope.launch {
            if (utteranceId != currentUtteranceId) return@launch
            when (event) {
                // A pause can land between speak() and the engine reporting start.
                TtsEngineEvent.START ->
                    if (_playbackState.value != TtsPlaybackState.PAUSED) {
                        _playbackState.value = TtsPlaybackState.PLAYING
                    }
                TtsEngineEvent.DONE -> complete(Outcome.DONE)
                TtsEngineEvent.STOP -> complete(Outcome.PAUSED)
                TtsEngineEvent.ERROR -> complete(Outcome.ERROR)
            }
        }
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
        // Leave PAUSED before the job runs: the loop's pause check must only observe a pause
        // that lands after this point (including while the first page's text is loading).
        _playbackState.value = TtsPlaybackState.PLAYING
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
                                stopSession()
                                return@launch
                            }
                        }
                    }
                    page++
                    chunk = 0
                }
                _playbackState.value = TtsPlaybackState.IDLE
                _currentUtterance.value = ""
                stopSession()
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
        readingJob = job
        startSession()
    }

    private suspend fun speakAndAwait(chunk: String): Outcome {
        if (!initialized) return Outcome.ERROR
        val id = "tts_${System.nanoTime()}"
        currentUtteranceId = id
        val deferred = CompletableDeferred<Outcome>()
        awaiting = deferred
        _playbackState.value = TtsPlaybackState.PLAYING
        if (!engine.speak(chunk, id)) {
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
        engine.stop()
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
        engine.stop()
        _playbackState.value = TtsPlaybackState.IDLE
        _currentUtterance.value = ""
        _currentPage.value = -1
        if (wasReading) stopSession()
    }

    fun setSpeed(rate: Float) {
        _speechRate.value = rate
        engine.setSpeechRate(rate)
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
        // Holds no context of its own: the engine and session lambdas capture the application
        // context, so the singleton cannot outlive the process.
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: TtsPlaybackController? = null

        fun get(context: Context): TtsPlaybackController = instance ?: synchronized(this) {
            instance ?: create(context.applicationContext).also { instance = it }
        }

        private fun create(appContext: Context): TtsPlaybackController = TtsPlaybackController(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            startSession = { TtsPlaybackService.ensureStarted(appContext) },
            stopSession = { TtsPlaybackService.stopSession(appContext) },
            engineFactory = { onReady, onEvent ->
                AndroidTtsSpeechEngine(appContext, onReady, onEvent)
            }
        )
    }
}
