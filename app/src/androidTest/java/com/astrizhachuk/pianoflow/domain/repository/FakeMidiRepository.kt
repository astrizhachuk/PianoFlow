package com.astrizhachuk.pianoflow.domain.repository

import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Фейковая реализация MidiRepository для управления состоянием подключения в UI-тестах.
 * Начальное состояние - NoDevice, что соответствует первому запуску приложения.
 */
class FakeMidiRepository : MidiRepository {

    private val connectionStateFlow = MutableStateFlow<ConnectionState>(ConnectionState.NoDevice)

    /**
     * Метод для симуляции различных состояний подключения из тестов.
     */
    fun emitState(state: ConnectionState) {
        connectionStateFlow.value = state
    }

    override fun observeConnectionState(): Flow<ConnectionState> {
        return connectionStateFlow
    }
}
