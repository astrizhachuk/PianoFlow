package com.astrizhachuk.pianoflow.data.datasource.midi

import com.astrizhachuk.pianoflow.domain.model.Note

/**
 * Parser for converting raw MIDI messages into domain models.
 */
class MidiMessageParser {

    /**
     * Parses a MIDI message.
     *
     * @param data The raw bytes of the MIDI message.
     * @return [Note] if the message is a 'Note On' event, otherwise `null`.
     */
    fun parse(data: ByteArray): Note? {
        if (data.isEmpty()) return null

        // Status byte of the MIDI message (first byte)
        val status = data[0].toInt() and 0xFF
        // Message command (upper 4 bits)
        val command = status and 0xF0

        // 0x90 is the Note On command
        // We only check the command, ignoring the channel (lower 4 bits)
        if (command == 0x90) {
            // Make sure the message contains data about the note and velocity
            if (data.size > 2) {
                val pitch = data[1].toInt()
                val velocity = data[2].toInt()

                // Consider the note "on" only if the velocity is greater than 0
                // (velocity 0 is often used as Note Off)
                if (velocity > 0) {
                    return Note(
                        pitch = pitch,
                        name = Note.pitchToName(pitch)
                    )
                }
            }
        }

        return null
    }
}
