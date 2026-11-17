package com.abhisheksati.mydoc.tools.tts

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TtsPlaybackControllerTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var engine: FakeEngine
    private lateinit var controller: TtsPlaybackController
    private var sessionsStarted = 0
    private var sessionsStopped = 0

    private class FakeEngine : TtsSpeechEngine {
        override val label = "Test engine"
        val spoken = mutableListOf<String>()
        val utteranceIds = mutableListOf<String>()
        private var onEvent: ((TtsEngineEvent, String?) -> Unit)? = null

        fun attach(listener: (TtsEngineEvent, String?) -> Unit) {
            onEvent = listener
        }

        override fun setSpeechRate(rate: Float) = Unit

        override fun speak(text: String, utteranceId: String): Boolean {
            spoken += text
            utteranceIds += utteranceId
            return true
        }

        override fun stop() = Unit

        override fun release() = Unit

        fun emit(event: TtsEngineEvent, utteranceId: String?) {
            onEvent?.invoke(event, utteranceId)
        }
    }

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        var onReady: ((Boolean) -> Unit)? = null
        controller = TtsPlaybackController(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
            startSession = { sessionsStarted++ },
            stopSession = { sessionsStopped++ },
            engineFactory = { ready, event ->
                onReady = ready
                FakeEngine().also { fake ->
                    engine = fake
                    fake.attach(event)
                }
            }
        )
        onReady!!.invoke(true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(TtsPlaybackState.IDLE, controller.playbackState.value)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `resume after pause speaks the paused chunk again`() {
        controller.startReading("Hello world")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf("Hello world"), engine.spoken)
        assertEquals(TtsPlaybackState.PLAYING, controller.playbackState.value)

        controller.pause()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(TtsPlaybackState.PAUSED, controller.playbackState.value)

        controller.resume()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, engine.spoken.size)
        assertEquals("Hello world", engine.spoken.last())
        assertEquals(TtsPlaybackState.PLAYING, controller.playbackState.value)
    }

    @Test
    fun `starting a new reading while paused begins speaking instead of staying paused`() {
        controller.startReading("First")
        testDispatcher.scheduler.advanceUntilIdle()
        controller.pause()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(TtsPlaybackState.PAUSED, controller.playbackState.value)

        controller.startReading("Second")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("First", "Second"), engine.spoken)
        assertEquals(TtsPlaybackState.PLAYING, controller.playbackState.value)
    }

    @Test
    fun `late error from a superseded utterance does not fail the current one`() {
        controller.startReading("First")
        testDispatcher.scheduler.advanceUntilIdle()
        val staleId = engine.utteranceIds.single()

        controller.startReading("Second")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, engine.spoken.size)

        engine.emit(TtsEngineEvent.ERROR, staleId)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(TtsPlaybackState.PLAYING, controller.playbackState.value)

        engine.emit(TtsEngineEvent.DONE, engine.utteranceIds.last())
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(TtsPlaybackState.IDLE, controller.playbackState.value)
    }

    @Test
    fun `stop then start reads the new request`() {
        controller.startReading("First")
        testDispatcher.scheduler.advanceUntilIdle()
        controller.stop()
        assertEquals(TtsPlaybackState.IDLE, controller.playbackState.value)

        controller.startReading("Second")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("First", "Second"), engine.spoken)
        assertEquals(TtsPlaybackState.PLAYING, controller.playbackState.value)
    }
}
