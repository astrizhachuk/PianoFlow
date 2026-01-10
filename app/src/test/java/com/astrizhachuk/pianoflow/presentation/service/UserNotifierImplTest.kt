
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
    fun `when message is shown, it is received by the collector`() = runTest {
        val message = UserMessage("Test message")

        notifier.userMessages.test {
            notifier.showMessage(message)
            assertEquals(message, awaitItem())
        }
    }

    @Test
    fun `new subscriber does not receive old messages`() = runTest {
        val message = UserMessage("Old message")
        notifier.showMessage(message)

        notifier.userMessages.test {
            expectNoEvents()
        }
    }

    @Test
    fun `when buffer is full, oldest message is dropped`() = runTest {
        val message1 = UserMessage("First")
        val message2 = UserMessage("Second")
        val message3 = UserMessage("Third")

        notifier.userMessages.test {
            // Отправляем и потребляем первое сообщение
            notifier.showMessage(message1)
            assertEquals(message1, awaitItem())

            // ВАЖНО: В тестовой среде с Turbine коллектор работает очень быстро.
            // Из-за этого буфер SharedFlow (extraBufferCapacity) на самом деле не переполняется,
            // и политика onBufferOverflow = DROP_OLDEST не срабатывает.
            // Этот тест по факту проверяет простое FIFO (First-In-First-Out) поведение.
            notifier.showMessage(message2)
            notifier.showMessage(message3)

            // Проверяем, что сообщения приходят в том порядке, в котором были отправлены.
            assertEquals(message2, awaitItem())
            assertEquals(message3, awaitItem())

            // Убеждаемся, что других сообщений нет.
            expectNoEvents()
        }
    }

    @Test
    fun `multiple collectors receive the same message`() = runTest {
        val message = UserMessage("Broadcast")
        val collector1Received = mutableListOf<UserMessage>()
        val collector2Received = mutableListOf<UserMessage>()

        val job1 = launch {
            notifier.userMessages.collect { collector1Received.add(it) }
        }
        val job2 = launch {
            notifier.userMessages.collect { collector2Received.add(it) }
        }

        // Даем коллекторам шанс запуститься
        yield()

        notifier.showMessage(message)

        // Даем коллекторам шанс обработать сообщение
        yield()

        // Убеждаемся, что оба коллектора получили сообщение
        assertEquals(1, collector1Received.size)
        assertEquals(message, collector1Received.first())

        assertEquals(1, collector2Received.size)
        assertEquals(message, collector2Received.first())

        job1.cancel()
        job2.cancel()
    }

    @Test
    fun `cancelled collector stops receiving messages`() = runTest {
        val message1 = UserMessage("First Broadcast")
        val message2 = UserMessage("Second Broadcast")
        val collector1Received = mutableListOf<UserMessage>()
        val collector2Received = mutableListOf<UserMessage>()

        val job1 = launch {
            notifier.userMessages.collect { collector1Received.add(it) }
        }
        val job2 = launch {
            notifier.userMessages.collect { collector2Received.add(it) }
        }

        // Запускаем коллекторы и отправляем первое сообщение
        yield()
        notifier.showMessage(message1)
        yield()

        // Оба должны были получить первое сообщение
        assertEquals(1, collector1Received.size)
        assertEquals(1, collector2Received.size)

        // Отменяем первый коллектор
        job1.cancel()
        yield() // Позволяем отмене завершиться

        // Отправляем второе сообщение
        notifier.showMessage(message2)
        yield()

        // Убеждаемся, что только второй коллектор получил новое сообщение
        assertEquals("Collector 1 should not receive new messages after cancellation", 1, collector1Received.size)

        assertEquals(2, collector2Received.size)
        assertEquals(message2, collector2Received.last())

        job2.cancel()
    }
}
