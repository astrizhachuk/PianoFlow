
package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import com.astrizhachuk.pianoflow.domain.model.Note
import timber.log.Timber

// Диапазон MIDI-нот, которые будут дублироваться на соседнем нотоносце (C2-C5)
private val GHOST_NOTE_RANGE = 36..72

/**
 * Преобразует список играемых нот в JSON-структуру для VexFlow,
 * разделяя их по нотоносцам и добавляя "призрачные" ноты в скобках.
 */
fun List<Note>.toVexflowJson(): String {
    Timber.d("toVexflowJson() called with notes: $this")
    if (this.isEmpty()) {
        return "{\"treble\":[], \"bass\":[]}"
    }

    // 1. Разделяем ноты на основные и те, для которых нужны "призраки"
    val primaryTrebleNotes = this.filter { it.pitch >= 60 }
    val primaryBassNotes = this.filter { it.pitch < 60 }

    val ghostNotesForTreble = primaryBassNotes.filter { it.pitch in GHOST_NOTE_RANGE }
    val ghostNotesForBass = primaryTrebleNotes.filter { it.pitch in GHOST_NOTE_RANGE }

    // 2. Собираем JSON-объекты для каждого стана
    val trebleNoteObjects = mutableListOf<String>()
    val bassNoteObjects = mutableListOf<String>()

    // Основной аккорд для скрипичного ключа
    if (primaryTrebleNotes.isNotEmpty()) {
        primaryTrebleNotes.mapNotNull { it.pitchToVexflow() }.takeIf { it.isNotEmpty() }?.let {
            trebleNoteObjects.add(createVexflowNoteJson(it, isGhost = false))
        }
    }
    // "Призрачный" аккорд для скрипичного ключа
    if (ghostNotesForTreble.isNotEmpty()) {
        ghostNotesForTreble.mapNotNull { it.pitchToVexflow() }.takeIf { it.isNotEmpty() }?.let {
            trebleNoteObjects.add(createVexflowNoteJson(it, isGhost = true))
        }
    }

    // Основной аккорд для басового ключа
    if (primaryBassNotes.isNotEmpty()) {
        primaryBassNotes.mapNotNull { it.pitchToVexflow() }.takeIf { it.isNotEmpty() }?.let {
            bassNoteObjects.add(createVexflowNoteJson(it, isGhost = false))
        }
    }
    // "Призрачный" аккорд для басового ключа
    if (ghostNotesForBass.isNotEmpty()) {
        ghostNotesForBass.mapNotNull { it.pitchToVexflow() }.takeIf { it.isNotEmpty() }?.let {
            bassNoteObjects.add(createVexflowNoteJson(it, isGhost = true))
        }
    }

    // 3. Формируем итоговую JSON-строку
    val result = "{\"treble\":${trebleNoteObjects.joinToString(",", "[", "]")}, \"bass\":${bassNoteObjects.joinToString(",", "[", "]")}}"
    Timber.d("toVexflowJson() result: $result")
    return result
}

private fun createVexflowNoteJson(keys: List<String>, isGhost: Boolean): String {
    val sortedKeys = keys.joinToString(separator = ", ") { "\"$it\"" }
    val ghostJson = if (isGhost) ", \"ghost\":true" else ""
    return "{\"keys\":[$sortedKeys], \"duration\":\"w\"$ghostJson}"
}

private fun Note.pitchToVexflow(): String? {
    if (pitch !in 0..127) {
        Timber.w("Invalid MIDI pitch value: $pitch.")
        return null
    }
    val noteNames = arrayOf("c", "c#", "d", "d#", "e", "f", "f#", "g", "g#", "a", "a#", "b")
    val octave = (pitch / 12) - 1
    val noteName = noteNames[pitch % 12]
    return "$noteName/$octave"
}
