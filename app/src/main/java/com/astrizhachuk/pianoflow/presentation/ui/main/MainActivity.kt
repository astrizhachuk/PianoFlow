package com.astrizhachuk.pianoflow.presentation.ui.main

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.astrizhachuk.pianoflow.presentation.ui.pianostaff.PianoStaffScreen
import com.astrizhachuk.pianoflow.presentation.service.UserNotifier
import com.astrizhachuk.pianoflow.presentation.viewmodel.midi.MidiConnectionViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MidiConnectionViewModel by viewModels()

    @Inject
    lateinit var userNotifier: UserNotifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PianoStaffScreen()
                }
            }
        }

        observeConnectionState()
        observeNotifications(findViewById<android.view.View>(android.R.id.content))
    }

    /**
     * Запускает отслеживание состояния MIDI-подключения.
     */
    private fun observeConnectionState() {
        viewModel.connectionState.launchIn(lifecycleScope)
    }

    /**
     * Подписывается на UI-уведомления от [UserNotifier] и отображает их в виде Snackbar.
     * Сбор данных происходит только когда Activity находится в состоянии STARTED или выше,
     * что предотвращает работу в фоновом режиме.
     */
    private fun observeNotifications(view: android.view.View) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                userNotifier.messages.collectLatest { message ->
                    Snackbar.make(view, message.text, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}
