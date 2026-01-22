package com.astrizhachuk.pianoflow.domain.model

/**
 * Represents the various states of a MIDI device connection.
 *
 * This sealed interface is used to model the different
 * possible outcomes when attempting to connect to or interact with a MIDI device.
 */
sealed interface ConnectionState {
    /**
     * Indicates a successful connection to a MIDI device.
     *
     * @property device Information about the connected device.
     */
    data class Connected(val device: MidiDevice) : ConnectionState

    /**
     * Indicates that the MIDI device has been disconnected.
     *
     * This state occurs when an active connection has been terminated.
     */
    data object Disconnected : ConnectionState

    /**
     * Indicates that an error occurred during the connection or interaction process.
     *
     * @property message A message describing the error.
     */
    data class Error(val message: String) : ConnectionState

    /**
     * Indicates that there are no available MIDI devices to connect to.
     *
     * This is the initial state or the state when all devices have been disconnected
     * and no new ones have been found.
     */
    data object NoDevice : ConnectionState
}
