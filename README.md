# PianoFlow

[Русская версия](docs/ru/README.md)

**PianoFlow** is an Android application designed for learning and practicing piano using MIDI devices.

## Target Audience

- Beginner pianists looking to improve their skills.
- Students in music schools and courses.
- Music enthusiasts who practice on electronic pianos with MIDI support.

## Core Purpose

PianoFlow serves as a **trainer and a system for verifying piano performance** on an instrument connected to an Android device via USB. Its main functions include:

- **MIDI Device Support via USB:** Works with electronic pianos and MIDI keyboards connected via USB.
- **Music File Integration:** Opens and plays various formats (like MIDI files) for practice sessions.
- **Interactive Training Modes:** Features different modes for practicing musical pieces, including a "wait" mode that tracks key presses in real-time, verifies the correct notes from a loaded file, and automatically proceeds to the next note upon correct input.

## Technical Specifications

- **Platform:** Android
- **Minimum SDK:** 23 (Android 6.0 Marshmallow)
- **Development Language:** Kotlin
- **MIDI API:** Utilizes the Android MIDI API for device communication.

## Features

The application includes the following features:

1.  **MIDI Piano Connection via USB:**
    -   Automatically detects connected MIDI devices.
    -   Establishes a connection with the selected device.

2.  **Working with Music Files:**
    -   Opens and loads various file formats (e.g., MIDI files).
    -   Parses and interprets musical data from files.
    -   Displays sheet music information to the user.

3.  **Training Modes:**
    -   **"Wait" Mode:** Checks for correct note presses against a loaded file on the MIDI device:
        -   The app waits for the correct note to be played.
        -   Moves to the next note upon a correct press.
        -   Monitors the sequence and accuracy of the performance.
    -   For better readability, a **Ghost Notes** feature displays notes in the crossover area on both staves simultaneously.

4.  **MIDI Message Processing:**
    -   Handles key press and release events.
    -   Parses MIDI messages (note, velocity, channel).
    -   Matches key presses on the MIDI device with notes from the loaded file.

5.  **User Feedback:**
    -   Provides visual indication of received MIDI messages.
    -   Displays information about the keys being pressed.
    -   Indicates the currently expected note from the file.
    -   Analyzes and provides an assessment of the performance.

