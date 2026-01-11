package com.astrizhachuk.pianoflow.presentation.ui.main

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.astrizhachuk.pianoflow.R
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
        setContentView(R.layout.activity_main)

        val mainView = findViewById<android.view.View>(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        observeConnectionState()
        observeNotifications(mainView)
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
                userNotifier.userMessages.collectLatest { message ->
                    Snackbar.make(view, message.text, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}
