package com.astrizhachuk.pianoflow.presentation.viewmodel

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.domain.usecase.midi.ShowConnectionNotificationUseCase
import com.astrizhachuk.pianoflow.domain.usecase.midi.TrackMidiConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для управления состоянием подключения MIDI-устройства.
 * Координирует работу Use Cases и отображение сообщений через Toast.
 */
@HiltViewModel
class MidiConnectionViewModel @Inject constructor(
    private val trackMidiConnectionUseCase: TrackMidiConnectionUseCase,
    private val showConnectionNotificationUseCase: ShowConnectionNotificationUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    /**
     * Состояние подключения MIDI-устройства.
     */
    val connectionState: StateFlow<ConnectionState> = trackMidiConnectionUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConnectionState.Disconnected
        )
    
    private var previousState: ConnectionState? = null
    
    init {
        // Инициализация отслеживания при создании ViewModel
        viewModelScope.launch {
            trackMidiConnectionUseCase.initialize()
        }
        
        // Наблюдение за изменениями состояния и отображение сообщений
        viewModelScope.launch {
            connectionState.collect { state ->
                handleStateChange(state)
            }
        }
    }
    
    /**
     * Обработка изменения состояния подключения.
     * Отображает Toast только при изменении состояния.
     */
    private fun handleStateChange(newState: ConnectionState) {
        // Пропускаем сообщение, если состояние не изменилось
        if (previousState == newState) {
            return
        }
        
        previousState = newState
        
        // Получаем сообщение для отображения
        val notificationMessage = showConnectionNotificationUseCase(newState)
        
        // Отображаем Toast, если сообщение есть
        notificationMessage?.let { message ->
            showToast(message.message)
        }
    }
    
    /**
     * Показать Toast с сообщением.
     */
    private fun showToast(message: String) {
        // Используем Main Thread для показа Toast
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}

