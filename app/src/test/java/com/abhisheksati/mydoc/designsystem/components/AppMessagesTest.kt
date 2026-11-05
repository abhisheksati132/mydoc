package com.abhisheksati.mydoc.designsystem.components

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMessagesTest {

    private fun drain() = runBlocking {
        while (withTimeoutOrNull(25) { AppMessages.next() } != null) {
            // Anything left from an earlier test must not be read as this test's result.
        }
    }

    @Test
    fun aMessageArrivingWhileAnotherIsOnScreenIsNotLost() = runBlocking {
        drain()
        AppMessages.show("3 moved, 1 could not be")
        AppMessages.show("the one that was left behind")

        assertEquals("3 moved, 1 could not be", AppMessages.next()?.text)
        assertEquals("the one that was left behind", AppMessages.next()?.text)
        assertEquals(null, withTimeoutOrNull(50) { AppMessages.next() })
    }

    @Test
    fun messagesComeOutInTheOrderTheyWentIn() = runBlocking {
        drain()
        val texts = listOf("one", "two", "three", "four", "five")
        texts.forEach { AppMessages.show(it) }
        val received = (1..texts.size).mapNotNull { AppMessages.next()?.text }
        assertEquals(texts, received)
    }

    @Test
    fun idsAreDistinctAndIncreasing() = runBlocking {
        drain()
        AppMessages.show("a")
        AppMessages.show("b")
        val first = AppMessages.next()
        val second = AppMessages.next()
        assertTrue("ids must differ or the host cannot tell them apart", first!!.id != second!!.id)
        assertTrue("ids must increase", second.id > first.id)
    }

    @Test
    fun nothingPendingMeansNoMessage() = runBlocking {
        drain()
        assertEquals(null, withTimeoutOrNull(50) { AppMessages.next() })
    }
}
