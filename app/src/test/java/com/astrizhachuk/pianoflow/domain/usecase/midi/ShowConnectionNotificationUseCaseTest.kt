package com.astrizhachuk.pianoflow.domain.usecase.midi

import com.astrizhachuk.pianoflow.domain.exception.MidiException
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.model.MidiDevice
import com.astrizhachuk.pianoflow.domain.model.NotificationMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit-тесты для ShowConnectionNotificationUseCase.
 */
class ShowConnectionNotificationUseCaseTest {
    
    private lateinit var useCase: ShowConnectionNotificationUseCase
    
    @Before
    fun setUp() {
        useCase = ShowConnectionNotificationUseCase()
    }
    
    @Test
    fun `invoke should return success notification for connected state`() {
        // Given
        val device = MidiDevice(id = 1, name = "Test Device", isInput = true)
        val state = ConnectionState.Connected(device)
        
        // When
        val result = useCase(state)
        
        // Then
        assertEquals("MIDI-клавиатура подключена", result?.message)
        assertEquals(NotificationMessage.NotificationType.SUCCESS, result?.type)
    }
    
    @Test
    fun `invoke should return info notification for disconnected state`() {
        // Given
        val state = ConnectionState.Disconnected
        
        // When
        val result = useCase(state)
        
        // Then
        assertEquals("MIDI-клавиатура отключена", result?.message)
        assertEquals(NotificationMessage.NotificationType.INFO, result?.type)
    }
    
    @Test
    fun `invoke should return null for connecting state`() {
        // Given
        val state = ConnectionState.Connecting
        
        // When
        val result = useCase(state)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `invoke should return error notification for device unavailable error`() {
        // Given
        val exception = MidiException.DeviceUnavailableException()
        val state = ConnectionState.Error(exception)
        
        // When
        val result = useCase(state)
        
        // Then
        assertEquals("Устройство недоступно. Проверьте подключение.", result?.message)
        assertEquals(NotificationMessage.NotificationType.ERROR, result?.type)
    }
    
    @Test
    fun `invoke should return error notification for permission denied error`() {
        // Given
        val exception = MidiException.PermissionDeniedException()
        val state = ConnectionState.Error(exception)
        
        // When
        val result = useCase(state)
        
        // Then
        assertEquals("Нет разрешения на доступ к MIDI-устройству.", result?.message)
        assertEquals(NotificationMessage.NotificationType.ERROR, result?.type)
    }
    
    @Test
    fun `invoke should return error notification for connection error`() {
        // Given
        val exception = MidiException.ConnectionException()
        val state = ConnectionState.Error(exception)
        
        // When
        val result = useCase(state)
        
        // Then
        assertEquals("Ошибка подключения к устройству. Попробуйте переподключить.", result?.message)
        assertEquals(NotificationMessage.NotificationType.ERROR, result?.type)
    }
    
    @Test
    fun `invoke should return error notification for device busy error`() {
        // Given
        val exception = MidiException.DeviceBusyException()
        val state = ConnectionState.Error(exception)
        
        // When
        val result = useCase(state)
        
        // Then
        assertEquals("Устройство уже используется другим приложением.", result?.message)
        assertEquals(NotificationMessage.NotificationType.ERROR, result?.type)
    }
    
    @Test
    fun `invoke should return error notification for midi not supported error`() {
        // Given
        val exception = MidiException.MidiNotSupportedException()
        val state = ConnectionState.Error(exception)
        
        // When
        val result = useCase(state)
        
        // Then
        assertEquals("MIDI не поддерживается на данном устройстве.", result?.message)
        assertEquals(NotificationMessage.NotificationType.ERROR, result?.type)
    }
    
    @Test
    fun `invoke should return error notification for unknown error`() {
        // Given
        val exception = MidiException.UnknownException()
        val state = ConnectionState.Error(exception)
        
        // When
        val result = useCase(state)
        
        // Then
        assertEquals("Произошла ошибка при подключении к устройству.", result?.message)
        assertEquals(NotificationMessage.NotificationType.ERROR, result?.type)
    }
}



