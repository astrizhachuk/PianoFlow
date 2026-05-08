package com.astrizhachuk.pianoflow.presentation.ui.main

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.astrizhachuk.pianoflow.presentation.model.UserMessage
import com.astrizhachuk.pianoflow.presentation.service.UserNotifier
import com.astrizhachuk.pianoflow.presentation.ui.pianostaff.PianoStaffScreen
import com.astrizhachuk.pianoflow.presentation.ui.theme.AppTheme
import com.astrizhachuk.pianoflow.presentation.viewmodel.midi.MidiConnectionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
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

        observeConnectionState()

        setContent {
            val snackbarHostState = remember { SnackbarHostState() }

            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize()
                        ) {
                            PianoStaffScreen(
                                modifier = Modifier.fillMaxSize()
                            )
                            ObserveNotifications(
                                messages = userNotifier.messages,
                                snackbarHostState = snackbarHostState
                            )
                        }
                    }
                    }
            }
        }
    }

    /**
     * Starts observing the MIDI connection state by launching a coroutine in the [lifecycleScope]
     * to collect updates from the [MidiConnectionViewModel.connectionState] flow.
     */
    private fun observeConnectionState() {
        viewModel.connectionState.launchIn(lifecycleScope)
    }

    /**
     * Observes a stream of user messages and displays them as snackbars.
     *
     * This composable uses [LaunchedEffect] to collect emissions from the [messages] flow
     * and updates the [snackbarHostState] to show UI notifications.
     *
     */
    @Composable
    private fun ObserveNotifications(
        messages: Flow<UserMessage>,
        snackbarHostState: SnackbarHostState
    ) {
        LaunchedEffect(messages, snackbarHostState) {
            messages.collectLatest { message ->
                snackbarHostState.showSnackbar(message = message.text)
            }
        }
    }
}