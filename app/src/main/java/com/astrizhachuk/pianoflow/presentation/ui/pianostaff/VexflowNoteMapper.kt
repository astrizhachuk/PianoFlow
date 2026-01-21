package com.astrizhachuk.pianoflow.presentation.ui.pianostaff

import com.astrizhachuk.pianoflow.domain.model.Note

/**
 * Преобразует список доменных моделей [Note] в строковое JSON-представление,
 * подходящее для отрисовки целой ноты или аккорда в VexFlow.
 *
 * Эта функция принимает список нот, сортирует их по высоте (pitch) и
 * форматирует в JSON-массив. Высота каждой ноты преобразуется в формат
 * VexFlow «нота/октава» (например, «C/4»). Полученный JSON представляет собой
 * один аккорд из целых нот («w»), содержащий все указанные ноты.
 *
 * Если входной список пуст, функция возвращает пустую строку JSON-массива «[]».
 *
 * @return JSON-строка для VexFlow. Пример вывода для нот C4 и E4:
 *         `"[{"keys": ["C/4", "E/4"], "duration": "w"}]"`
 */
fun List<Note>.toVexflowJson(): String {
    if (this.isEmpty()) return "[]"

    val keys = this.sortedBy { it.pitch }.joinToString(separator = ", ") { note ->
        "\"${note.pitchToVexflow()}\""
    }
    return "[{\"keys\": [$keys], \"duration\": \"w\"}]"
}

private fun Note.pitchToVexflow(): String {
    val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val octave = (this.pitch / 12) - 1
    val noteName = noteNames[this.pitch % 12]
    return "$noteName/$octave"
}
