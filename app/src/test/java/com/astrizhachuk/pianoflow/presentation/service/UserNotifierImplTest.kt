package com.astrizhachuk.pianoflow.presentation.service

import app.cash.turbine.test
import com.astrizhachuk.pianoflow.presentation.model.UserMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UserNotifierImplTest {

    private lateinit var notifier: UserNotifierImpl

    @Before
    fun setUp() {
        notifier = UserNotifierImpl()
    }

    @Test
    fun `when message is sent, it is received by the collector`() = runTest {
        val message = UserMessage("Test message")

        notifier.messages.test {
            notifier.sendMessage(message)
            assertEquals(message, awaitItem())
        }
    }

    @Test
    fun `new subscriber does not receive old messages`() = runTest {
        val message = UserMessage("Old message")
        notifier.sendMessage(message)

        notifier.messages.test {
            expectNoEvents()
        }
    }

    @Test
    fun `when buffer is full, oldest message is dropped`() = runTest {
        val message1 = UserMessage("First")
        val message2 = UserMessage("Second")
        val message3 = UserMessage("Third")
        val receivedItems = mutableListOf<UserMessage>()

        backgroundScope.launch {
            notifier.messages.collect {
                receivedItems.add(it)
            }
        }

        yield()

        notifier.sendMessage(message1)
        notifier.sendMessage(message2)
        notifier.sendMessage(message3)

        yield()

        assertEquals(
            "The received messages should match the expected ones",
            listOf(message3),
            receivedItems
        )
    }

    @Test
    fun `multiple collectors receive the same message`() = runTest {
        val message = UserMessage("Broadcast")
        val collector1Received = mutableListOf<UserMessage>()
        val collector2Received = mutableListOf<UserMessage>()

        backgroundScope.launch { notifier.messages.collect { collector1Received.add(it) } }
        backgroundScope.launch { notifier.messages.collect { collector2Received.add(it) } }

        yield()

        notifier.sendMessage(message)

        yield()

        assertEquals(1, collector1Received.size)
        assertEquals(message, collector1Received.first())

        assertEquals(1, collector2Received.size)
        assertEquals(message, collector2Received.first())
    }

    @Test
    fun `cancelled collector stops receiving messages`() = runTest {
        val message1 = UserMessage("First Broadcast")
        val message2 = UserMessage("Second Broadcast")
        val collector1Received = mutableListOf<UserMessage>()
        val collector2Received = mutableListOf<UserMessage>()

        val job1 = backgroundScope.launch {
            notifier.messages.collect { collector1Received.add(it) }
        }
        backgroundScope.launch {
            notifier.messages.collect { collector2Received.add(it) }
        }

        yield()

        notifier.sendMessage(message1)

        yield()

        assertEquals(1, collector1Received.size)
        assertEquals(1, collector2Received.size)

        job1.cancel()

        yield()

        notifier.sendMessage(message2)

        yield()

        assertEquals(
            "Collector 1 should not receive new messages after cancellation",
            1,
            collector1Received.size
        )

        assertEquals(2, collector2Received.size)
        assertEquals(message2, collector2Received.last())
    }
}
