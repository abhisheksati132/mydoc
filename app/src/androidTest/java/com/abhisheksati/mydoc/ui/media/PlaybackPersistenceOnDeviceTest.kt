package com.abhisheksati.mydoc.ui.media

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.data.local.AppDatabase
import com.abhisheksati.mydoc.data.local.entity.MediaPlaybackEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * N-3 against the real database. The players saved from the scope that is being cancelled as the
 * screen leaves composition; this reproduces that pattern to show it does not land, and then drives
 * the checkpoint writer the way the poll loop does to show the position survives.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackPersistenceOnDeviceTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun stored(uri: String): MediaPlaybackEntity? =
        database.mediaPlaybackDao().getPlaybackPosition(uri)

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aSaveStartedFromOnDisposeWithTheCompositionScopeNeverLands() {
        // The mechanism the players used to rely on, tested as it actually is: the scope handed to
        // a composable by rememberCoroutineScope() is cancelled while the composition is being torn
        // down, and onDispose runs inside that teardown.
        val uri = "content://mydoc.test/dispose-scope-song"
        val playerVisible = androidx.compose.runtime.mutableStateOf(true)
        var disposed = false
        compose.setContent {
            if (playerVisible.value) {
                val scope = rememberCoroutineScope()
                DisposableEffect(Unit) {
                    onDispose {
                        disposed = true
                        scope.launch {
                            database.mediaPlaybackDao().savePlaybackPosition(
                                MediaPlaybackEntity(uri, 45_000L, 120_000L, System.currentTimeMillis())
                            )
                        }
                    }
                }
                Text("now playing")
            } else {
                Text("somewhere else")
            }
        }
        compose.waitForIdle()
        assertTrue("the player never entered composition", !disposed)

        // Leave the screen.
        playerVisible.value = false
        compose.waitForIdle()
        assertTrue("onDispose never ran", disposed)
        Thread.sleep(500L)

        assertNull(
            "the write launched from onDispose landed, so the old players were not losing resume " +
                "positions this way and N-3's premise needs restating",
            runBlocking { stored(uri) }
        )
    }

    @Test
    fun aSessionDrivenByCheckpointsLeavesTheReachedPositionBehind() {
        val uri = "content://mydoc.test/checkpointed-song"
        runBlocking {
            val writer = PlaybackCheckpointWriter { value, duration ->
                database.mediaPlaybackDao().savePlaybackPosition(
                    MediaPlaybackEntity(uri, value, duration, System.currentTimeMillis())
                )
            }
            // Forty seconds of playback at the player's own polling rate, then the listener stops.
            var position = 0L
            while (position <= 40_000L) {
                writer.checkpoint(position, 120_000L)
                position += 250L
            }
            writer.checkpoint(41_200L, 120_000L, force = true)
        }

        val row = runBlocking { stored(uri) }
        assertNotNull("no resume position was stored", row)
        assertEquals(41_200L, row!!.positionMs)
        assertEquals(120_000L, row.durationMs)
    }

    @Test
    fun reopeningTheSameTrackResumesFromTheStoredPosition() {
        val uri = "content://mydoc.test/reopened-song"
        runBlocking {
            val firstSession = PlaybackCheckpointWriter { value, duration ->
                database.mediaPlaybackDao().savePlaybackPosition(
                    MediaPlaybackEntity(uri, value, duration, System.currentTimeMillis())
                )
            }
            firstSession.checkpoint(70_000L, 180_000L)
            firstSession.checkpoint(72_500L, 180_000L, force = true)

            val stored = stored(uri)
            assertNotNull(stored)
            val resume = PlaybackCheckpoints.valueToSave(stored!!.positionMs, stored.durationMs)
            assertEquals(72_500L, resume)

            // Second open: read, seed, and confirm the resume position is not rewritten or lost.
            val secondSession = PlaybackCheckpointWriter { value, duration ->
                database.mediaPlaybackDao().savePlaybackPosition(
                    MediaPlaybackEntity(uri, value, duration, System.currentTimeMillis())
                )
            }
            secondSession.seed(resume!!)
            assertNull(secondSession.checkpoint(resume, stored.durationMs))
            assertEquals(resume, stored(uri)?.positionMs)
        }
    }

    @Test
    fun aTrackPlayedToTheEndIsStoredAtTheStartForTheNextListen() {
        val uri = "content://mydoc.test/finished-song"
        runBlocking {
            val writer = PlaybackCheckpointWriter { value, duration ->
                database.mediaPlaybackDao().savePlaybackPosition(
                    MediaPlaybackEntity(uri, value, duration, System.currentTimeMillis())
                )
            }
            writer.checkpoint(100_000L, 120_000L)
            writer.checkpoint(119_500L, 120_000L, force = true)
        }
        assertEquals(0L, runBlocking { stored(uri) }?.positionMs)
    }
}
