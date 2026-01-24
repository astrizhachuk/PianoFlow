
package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import com.astrizhachuk.pianoflow.domain.model.Note
import timber.log.Timber

// Диапазон MIDI-нот, которые будут дублироваться на соседнем нотоносце (C2-C5)
private val GHOST_NOTE_RANGE = 36..72

/**
 * Преобразует список играемых нот в JSON-структуру для VexFlow.
 *
 * Эта функция разделяет ноты по нотоносцам (скрипичный и басовый) и добавляет
 * дублирующие "призрачные" ноты для тех, что находятся в пограничной зоне.
 * "Призрачные" ноты помечаются специальным флагом, чтобы их можно было отрисовать
 * другим цветом на фронтенде.
 */
fun List<Note>.toVexflowJson(): String {
    Timber.d("toVexflowJson() called with notes: $this")
    if (this.isEmpty()) {
        return "{\"treble\":[], \"bass\":[]}"
    }

    // 1. Разделяем все ноты по их основному нотоносцу
    val trebleNotes = this.filter { it.pitch >= 60 }
    val bassNotes = this.filter { it.pitch < 60 }

    // 2. Находим "призрачные" ноты, которые нужно дублировать на соседних станах
    val ghostNotesForTreble = bassNotes.filter { it.pitch in GHOST_NOTE_RANGE }
    val ghostNotesForBass = trebleNotes.filter { it.pitch in GHOST_NOTE_RANGE }

    // 3. Формируем JSON-объекты для каждого стана
    val trebleNoteObjects = createNoteObjects(primary = trebleNotes, ghost = ghostNotesForTreble)
    val bassNoteObjects = createNoteObjects(primary = bassNotes, ghost = ghostNotesForBass)

    // 4. Собираем итоговую JSON-строку
    val result = "{\"treble\":$trebleNoteObjects, \"bass\":$bassNoteObjects}"
    Timber.d("toVexflowJson() result: $result")
    return result
}

/**
 * Создает JSON-представление нот для одного нотоносца.
 */
private fun createNoteObjects(primary: List<Note>, ghost: List<Note>): String {
    val objects = mutableListOf<String>()
    primary.mapNotNull { it.pitchToVexflow() }.takeIf { it.isNotEmpty() }?.let {
        objects.add(createVexflowNoteJson(it, isGhost = false))
    }
    ghost.mapNotNull { it.pitchToVexflow() }.takeIf { it.isNotEmpty() }?.let {
        objects.add(createVexflowNoteJson(it, isGhost = true))
    }
    return objects.joinToString(separator = ",", prefix = "[", postfix = "]")
}

/**
 * Создает JSON-строку для одного аккорда (обычного или призрачного).
 */
private fun createVexflowNoteJson(keys: List<String>, isGhost: Boolean): String {
    val keysJson = keys.joinToString(separator = ", ") { "\"$it\"" }
    val ghostJson = if (isGhost) ", \"ghost\":true" else ""
    return "{\"keys\":[$keysJson], \"duration\":\"w\"$ghostJson}"
}

/**
 * Преобразует высоту MIDI-ноты в строковый формат VexFlow ("нота/октава").
 */
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
