package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import com.astrizhachuk.pianoflow.domain.model.Note
import timber.log.Timber

/**
 * Преобразует список доменных моделей [Note] в строковое JSON-представление,
 * подходящее для отрисовки целой ноты или аккорда в VexFlow.
 *
 * Эта функция-расширение принимает список нот, сортирует их по высоте (pitch)
 * и форматирует в JSON-массив. Высота каждой ноты преобразуется в формат
 * VexFlow «нота/октава» (например, «c/4»). Полученный JSON представляет собой
 * один аккорд из целых нот («w»), содержащий все указанные ноты.
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

    // Создаем строку вида: "\"c#/4\"", "\"e/4\""
    val keys = this.sortedBy { it.pitch }.joinToString(separator = ", ") { note ->
        val vexflowPitch = note.pitchToVexflow()
        if (vexflowPitch.isNotEmpty()) {
            "\\\"$vexflowPitch\\\""
        } else {
            null
        }
    }.filterNotNull()

    if (keys.isEmpty()) {
        Timber.w("No valid notes to convert to VexFlow JSON.")
        return "[]"
    }

    // Собираем итоговую JSON-строку, экранируя все внутренние кавычки.
    // Двойной бэкслеш (\\) в Kotlin-строке превращается в один бэкслеш (\) в итоговой строке.
    val result = "[{\\\"keys\\\":[${keys.joinToString()}],\\\"duration\\\":\\\"w\\\"}]"
    Timber.d("toVexflowJson() result: $result")
    return result
}

private fun Note.pitchToVexflow(): String {
    if (pitch !in 0..127) {
        Timber.e("Invalid MIDI pitch value: $pitch. Must be in range 0-127.")
        return ""
    }
    val noteNames = arrayOf("c", "c#", "d", "d#", "e", "f", "f#", "g", "g#", "a", "a#", "b")
    val octave = (this.pitch / 12) - 1
    val noteName = noteNames[this.pitch % 12]
    return "$noteName/$octave"
}
