package com.astrizhachuk.pianoflow.presentation.ui.main

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.astrizhachuk.pianoflow.R
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
class MainActivity : ComponentActivity() {

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
                        topBar = { PianoFlowTopBar() },
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

    private fun observeConnectionState() {
        viewModel.connectionState.launchIn(lifecycleScope)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PianoFlowTopBar() {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }

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
