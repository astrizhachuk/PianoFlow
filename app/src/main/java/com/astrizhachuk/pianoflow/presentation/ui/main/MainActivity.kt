package com.astrizhachuk.pianoflow.presentation.ui.main

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.astrizhachuk.pianoflow.R
import com.astrizhachuk.pianoflow.presentation.service.UserNotifier
import com.astrizhachuk.pianoflow.presentation.viewmodel.midi.MidiConnectionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Принудительная инициализация ViewModel
        lifecycleScope.launch {
            viewModel.connectionState.collect()
        }

        // Наблюдение за уведомлениями от UserNotifier
        observeNotifications()
    }

    private fun observeNotifications() {
        lifecycleScope.launch {
            userNotifier.userMessages.collectLatest { message ->
                Toast.makeText(this@MainActivity, message.text, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
