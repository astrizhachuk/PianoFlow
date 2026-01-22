package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import com.astrizhachuk.pianoflow.domain.model.Note
import timber.log.Timber

/**
 * Преобразует список доменных моделей [Note] в строковое JSON-представление,
 * подходящее для отрисовки целой ноты или аккорда в VexFlow.
 *
 * Эта функция-расширение принимает список нот, сортирует их по высоте (pitch)
 * и форматирует в JSON-массив. Невалидные ноты (с высотой звука вне диапазона 0-127)
 * игнорируются.
 *
 * Важно: Эта функция создает строку с экранированными кавычками, чтобы ее можно
 * было безопасно передать в JavaScript-функцию, ожидающую строковый аргумент.
 *
 * @return Экранированная JSON-строка для VexFlow. Пример вывода для ноты C#4:
 *         `"[{\"keys\":[\"c#/4\"],\"duration\":\"w\"}]"`
 */
fun List<Note>.toVexflowJson(): String {
    Timber.d("toVexflowJson() called with notes: $this")
    if (this.isEmpty()) {
        Timber.w("Input note list is empty, returning empty VexFlow JSON.")
        return "[]"
    }

    val keys = this.sortedBy { it.pitch }
        .mapNotNull { note -> note.pitchToVexflow() }
        .joinToString(separator = ", ") { vexflowNote ->
            "\\\"$vexflowNote\\\""
        }

    if (keys.isEmpty()) {
        Timber.w("No valid notes found to convert to VexFlow JSON. Input was: $this")
        return "[]"
    }

    // Собираем итоговую JSON-строку, экранируя все внутренние кавычки.
    val result = "[{\\\"keys\\\":[$keys],\\\"duration\\\":\\\"w\\\"}]"
    Timber.d("toVexflowJson() result: $result")
    return result
}

/**
 * Преобразует высоту MIDI-ноты в строковый формат VexFlow ("нота/октава").
 *
 * Если значение pitch находится вне допустимого диапазона MIDI (0-127), функция
 * возвращает `null`.
 *
 * @return Строка в формате VexFlow (например, "c#/4") для валидных нот или `null` для невалидных.
 */
private fun Note.pitchToVexflow(): String? {
    if (pitch !in 0..127) {
        Timber.w("Invalid MIDI pitch value: $pitch. Must be in range 0-127. Note will be ignored.")
        return null
    }
    val noteNames = arrayOf("c", "c#", "d", "d#", "e", "f", "f#", "g", "g#", "a", "a#", "b")
    val octave = (pitch / 12) - 1
    val noteName = noteNames[pitch % 12]
    return "$noteName/$octave"
}
