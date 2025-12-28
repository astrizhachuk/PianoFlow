package com.astrizhachuk.pianoflow

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import com.astrizhachuk.pianoflow.presentation.viewmodel.MidiConnectionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Главная Activity приложения.
 * Инициализирует отслеживание подключения MIDI-устройств.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private val viewModel: MidiConnectionViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Наблюдение за состоянием подключения (опционально, для отладки)
        lifecycleScope.launch {
            viewModel.connectionState.collect { state ->
                // Можно добавить логирование или обновление UI
                when (state) {
                    is ConnectionState.Connected -> {
                        // Устройство подключено
                    }
                    is ConnectionState.Disconnected -> {
                        // Устройство отключено
                    }
                    is ConnectionState.Error -> {
                        // Ошибка подключения
                    }
                    is ConnectionState.Connecting -> {
                        // Идет подключение
                    }
                }
            }
        }
    }
}