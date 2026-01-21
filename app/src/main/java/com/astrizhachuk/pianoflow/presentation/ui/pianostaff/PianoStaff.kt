package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.astrizhachuk.pianoflow.domain.model.Note

//@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PianoStaff(
    notes: List<Note>,
    modifier: Modifier = Modifier
) {
    // --- ИЗМЕНЕНИЕ ---
    // Разделяем ноты на два списка: для басового и скрипичного ключей.
    // Нота "до" первой октавы (Middle C, pitch 60) и выше идут в скрипичный ключ.
    val (bassNotes, trebleNotes) = remember(notes) {
        notes.partition { it.pitch < 60 }
    }

    val trebleNotesJson by remember(trebleNotes) {
        mutableStateOf(createNotesJson(trebleNotes))
    }

    val bassNotesJson by remember(bassNotes) {
        mutableStateOf(createNotesJson(bassNotes))
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                webChromeClient = WebChromeClient()
                loadUrl("file:///android_asset/vexflow.html")
            }
        },
        update = { webView ->
            // Передаем два списка нот в JavaScript для отрисовки на двух станах.
            webView.evaluateJavascript("drawNotes('$trebleNotesJson', '$bassNotesJson')", null)
        },
        modifier = modifier
    )
}

// Вспомогательная функция для создания JSON из списка нот.
private fun createNotesJson(notes: List<Note>): String {
    return if (notes.isEmpty()) {
        "[]"
    } else {
        val sortedNotes = notes.sortedBy { it.pitch }
        val keys = sortedNotes.joinToString(separator = ", ") { note ->
            "\"${pitchToVexflow(note.pitch)}\""
        }
        // Аккорд по-прежнему является одной целой нотой ("w").
        "[{\"keys\": [$keys], \"duration\": \"w\"}]"
    }
}

private fun pitchToVexflow(pitch: Int): String {
    val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val octave = (pitch / 12) - 1
    val noteName = noteNames[pitch % 12]
    return "$noteName/$octave"
}

@Preview
@Composable
private fun PianoStaffPreview() {
    MaterialTheme {
        PianoStaff(
            notes = listOf(
                Note(pitch = 60),
                Note(pitch = 64),
                Note(pitch = 67)
            )
        )
    }
}
