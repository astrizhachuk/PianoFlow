# MIDI API в Android

Пакет `android.media.midi` предоставляет все необходимые инструменты для взаимодействия с MIDI-устройствами (Musical Instrument Digital Interface). Он позволяет приложениям обнаруживать, настраивать и обмениваться данными с MIDI-клавиатурами, синтезаторами и другими контроллерами, подключенными через USB, Bluetooth Low Energy (BLE) или работающими как виртуальные (программные) устройства.

## Начало работы: Пошаговое руководство

Ниже описана последовательность действий для подключения и взаимодействия с MIDI-устройством.

### 1. Декларирование в манифесте

Чтобы ваше приложение было доступно в Google Play только для устройств, поддерживающих MIDI, необходимо добавить в `AndroidManifest.xml` следующее объявление:

```xml
<uses-feature android:name="android.software.midi" android:required="true" />
```

Это указывает на требование **программной** поддержки MIDI API (начиная с Android 6.0), а не редких аппаратных MIDI-портов.

### 2. Проверка поддержки в коде

Хотя декларирование в манифесте предотвратит установку приложения из Google Play на несовместимые устройства, рекомендуется также выполнять проверку на поддержку MIDI в коде. Это полезно на случай, если приложение установлено другими способами, или для более грациозной деградации функционала.

```kotlin
if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)) {
    // MIDI поддерживается, можно продолжать работу
} else {
    // MIDI не поддерживается на этом устройстве
    // Сообщите пользователю или отключите MIDI-функционал
}
```

### 3. Получение `MidiManager`

`MidiManager` — это отправная точка для работы с MIDI. Он предоставляет доступ ко всем остальным функциям API.

```kotlin
val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
```

### 4. Выбор устройства (`MidiDevice`)

Подключенные устройства представлены классом `MidiDevice`. Чтобы получить их список, используется `MidiManager`:

```kotlin
val devices: Array<MidiDeviceInfo> = midiManager.devices
```

Каждый элемент `MidiDeviceInfo` содержит метаданные об устройстве: свойства (имя, производитель), а также количество и типы портов.

### 5. Отслеживание подключения и отключения

Для динамического отслеживания устройств используется `MidiManager.DeviceCallback`.

```kotlin
midiManager.registerDeviceCallback(object : MidiManager.DeviceCallback() {
    override fun onDeviceAdded(device: MidiDeviceInfo) {
        // Логика при подключении нового устройства
    }
    override fun onDeviceRemoved(device: MidiDeviceInfo) {
        // Логика при отключении устройства
    }
}, Handler(Looper.getMainLooper()))
```

### 6. Открытие устройства и работа с портами

Для обмена данными устройство необходимо открыть. Этот процесс асинхронен:

```kotlin
midiManager.openDevice(deviceInfo, {
    // Этот коллбэк вызовется, когда устройство будет готово
    val midiDevice = it
    if (midiDevice == null) {
        // Обработка ошибки открытия
    } else {
        // Устройство успешно открыто
    }
}, null)
```

После открытия вы получаете доступ к портам устройства. Порты бывают двух типов:
*   **Входные (`InputPort`)**: для **получения** данных от устройства (например, нажатия клавиш).
*   **Выходные (`OutputPort`)**: для **отправки** данных на устройство (например, для проигрывания ноты на синтезаторе).

### 7. Чтение данных с устройства

Для чтения данных нужно открыть входной порт и передать ему реализацию класса `MidiReceiver`. Это абстрактный класс, который вы должны унаследовать, чтобы обрабатывать входящие MIDI-сообщения.

```kotlin
// 1. Создаем свой класс для обработки MIDI-сообщений
class MyMidiReceiver : MidiReceiver() {
    override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
        // Здесь вы обрабатываете входящий MIDI-пакет (массив байт)
    }
}

// 2. Открываем входной порт и подключаем к нему наш обработчик
val inputPort = midiDevice.openInputPort(portNumber)
inputPort.connect(MyMidiReceiver())
```

### 8. Отправка данных на устройство

Для отправки данных открывается выходной порт. В него можно напрямую записывать байтовые массивы с MIDI-командами.

```kotlin
val outputPort = midiDevice.openOutputPort(portNumber)
val buffer = ByteArray(32)
// ...заполняем buffer MIDI-командой...
outputPort.send(buffer, 0, buffer.size)
```

Не забудьте закрывать порты (`port.close()`) и устройства (`device.close()`), когда они больше не нужны.

## Создание виртуального MIDI-устройства

Android MIDI API позволяет вашему приложению самому стать MIDI-устройством, к которому смогут подключаться другие приложения (например, DAW или секвенсоры). Для этого необходимо:

1.  **Создать сервис**, наследующий `MidiDeviceService`.
2.  **Переопределить метод `onGetInputPortReceivers()`**. Этот метод должен вернуть массив `MidiReceiver`, которые будут обрабатывать данные, отправленные вашему виртуальному устройству другими приложениями.
3.  **Объявить сервис в `AndroidManifest.xml`** с соответствующим интент-фильтром:

    ```xml
    <service android:name=".MyMidiDeviceService"
             android:permission="android.permission.BIND_MIDI_DEVICE_SERVICE">
        <intent-filter>
            <action android:name="android.media.midi.MidiDeviceService" />
        </intent-filter>
    </service>
    ```

## Ключевые классы пакета

*   `MidiManager`: Центральный класс для обнаружения и управления устройствами.
*   `MidiDevice`: Представляет реальное или виртуальное MIDI-устройство.
*   `MidiDeviceInfo`: Содержит информацию об устройстве (свойства, порты).
*   `MidiInputPort` / `MidiOutputPort`: Классы для чтения и записи MIDI-данных.
*   `MidiReceiver`: Абстрактный класс, который вы реализуете для приема MIDI-данных.
*   `MidiSender`: Интерфейс для классов, которые могут отправлять MIDI-данные (например, `MidiInputPort`).
*   `MidiDeviceService`: Базовый класс для создания виртуальных MIDI-устройств.

## Для приложений, критичных к задержке (C/C++)

Для приложений, где важна минимальная задержка (real-time синтезаторы, профессиональные DAW), рекомендуется использовать **Native MIDI API (AMidi)**, доступный через NDK. Он позволяет обрабатывать MIDI-сообщения в C/C++ коде, что значительно снижает задержки. При этом для обнаружения и подключения устройств по-прежнему используется `MidiManager` из Java/Kotlin.

## Официальные ресурсы и ссылки

*   **Обзор пакета android.media.midi**:
    *   [https://developer.android.com/reference/android/media/midi/package-summary](https://developer.android.com/reference/android/media/midi/package-summary)
*   **Официальная документация по MidiManager**:
    *   [https://developer.android.com/reference/android/media/midi/MidiManager](https://developer.android.com/reference/android/media/midi/MidiManager)
*   **Гайд по Native MIDI API (для C/C++)**:
    *   [https://developer.android.com/ndk/guides/audio/midi](https://developer.android.com/ndk/guides/audio/midi)
*   **Официальные примеры (Best Practices)**:
    *   [https://github.com/android/midi-samples](https://github.com/android/midi-samples)
