# PianoFlow

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
    -   Other training modes are planned for future development.

4.  **MIDI Message Processing:**
    -   Handles key press and release events.
    -   Parses MIDI messages (note, velocity, channel).
    -   Matches key presses on the MIDI device with notes from the loaded file.

5.  **User Feedback:**
    -   Provides visual indication of received MIDI messages.
    -   Displays information about the keys being pressed.
    -   Indicates the currently expected note from the file.
    -   Analyzes and provides an assessment of the performance.

---

# PianoFlow

**PianoFlow** — Android-приложение, предназначенное для обучения и тренировки игры на фортепиано с использованием MIDI-устройств.

## Целевая аудитория

- Начинающие пианисты, желающие улучшить свои навыки
- Учащиеся музыкальных школ и курсов
- Любители музыки, практикующие игру на электронном пианино с MIDI-поддержкой

## Основное предназначение

Приложение PianoFlow представляет собой **тренажер и систему проверки игры на пианино**, подключенном через USB к Android-устройству. Основные функции:

- **Поддержка MIDI-устройств через USB** — работа с электронными пианино и MIDI-клавиатурами, подключенными через USB
- **Работа с музыкальными файлами** — открытие и проигрывание различных форматов (MIDI-файлы, различные форматы хранения нот и т.д.) для тренировки
- **Режимы тренировки с проверкой игры** — различные режимы работы с музыкальными произведениями, включая режим "ожидания": отслеживание нажатий клавиш в реальном времени, проверка правильности нажатия указанных нот из файла, автоматический переход к следующей ноте при правильном нажатии

## Технические характеристики

- **Платформа**: Android
- **Минимальная версия SDK**: 23 (Android 6.0 Marshmallow)
- **Язык разработки**: Kotlin
- **Поддержка MIDI API**: Использование Android MIDI API для работы с MIDI-устройствами

## Функционал

Приложение будет включать:

1. **Подключение MIDI-пианино через USB**
   - Автоматическое обнаружение подключенных MIDI-устройств
   - Установление соединения с устройством

2. **Работа с музыкальными файлами**
   - Открытие и загрузка различных форматов файлов (MIDI-файлы, различные форматы хранения нот и т.д.)
   - Парсинг и интерпретация музыкальных данных из файлов
   - Отображение нотной информации для пользователя

3. **Режимы тренировки**
   - **Режим "ожидания"** — проверка правильности нажатия указанных нот в файлах и на MIDI-устройстве:
     - Приложение ожидает нажатия правильной ноты согласно загруженному файлу
     - При правильном нажатии происходит переход к следующей ноте
     - Контроль последовательности и точности исполнения
   - Другие режимы тренировки (детали будут уточнены в процессе разработки)

4. **Прием и обработка MIDI-сообщений**
   - Обработка событий нажатия и отпускания клавиш
   - Парсинг MIDI-сообщений (ноты, velocity, каналы)
   - Сопоставление нажатий на MIDI-устройстве с нотами из загруженного файла

5. **Обратная связь пользователю**
   - Визуальная индикация полученных MIDI-сообщений
   - Отображение информации о нажатых клавишах
   - Индикация текущей ожидаемой ноты из файла
   - Анализ и оценка игры