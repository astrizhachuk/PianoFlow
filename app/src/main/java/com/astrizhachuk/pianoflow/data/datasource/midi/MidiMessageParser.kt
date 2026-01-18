package com.astrizhachuk.pianoflow.data.datasource.midi

import com.astrizhachuk.pianoflow.domain.model.Note

/**
 * Парсер для преобразования сырых MIDI-сообщений в доменные модели.
 */
class MidiMessageParser {

    /**
     * Разбирает MIDI-сообщение.
     *
     * @param data Сырые байты MIDI-сообщения.
     * @return [Note], если сообщение является событием 'Note On', иначе `null`.
     */
    fun parse(data: ByteArray): Note? {
        if (data.isEmpty()) return null

        // Статус-байт MIDI-сообщения (первый байт)
        val status = data[0].toInt() and 0xFF
        // Команда сообщения (старшие 4 бита)
        val command = status and 0xF0

        // 0x90 - команда Note On
        // Мы проверяем только команду, игнорируя канал (младшие 4 бита)
        if (command == 0x90) {
            // Убедимся, что сообщение содержит данные о ноте и силе нажатия
            if (data.size > 2) {
                val pitch = data[1].toInt()
                val velocity = data[2].toInt()

                // Считаем ноту "включенной", только если сила нажатия больше 0
                // (velocity 0 часто используется как Note Off)
                if (velocity > 0) {
                    return Note(pitch = pitch)
                }
            }
        }

        return null
    }
}
