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

После открытия вы получаете доступ к портам устройства. 

> **Важное замечание о терминологии портов:**
> В Android MIDI API существует очень неинтуитивная, но критически важная особенность именования портов. Они названы **с точки зрения вашего приложения**, а не внешнего устройства.
> *   **`MidiOutputPort` (Выходной порт):** Используется для **получения** данных *от* MIDI-устройства. Вы открываете его и `connect()`ите к нему свой `MidiReceiver`.
> *   **`MidiInputPort` (Входной порт):** Используется для **отправки** данных *на* MIDI-устройство. Вы открываете его и вызываете `send()`.
> 
> Эта инверсия — частый источник ошибок.

### 7. Чтение данных с устройства

Для чтения данных нужно открыть **выходной порт** и передать ему реализацию класса `MidiReceiver`. Это абстрактный класс, который вы должны унаследовать, чтобы обрабатывать входящие MIDI-сообщения.

```kotlin
// 1. Создаем свой класс для обработки MIDI-сообщений
class MyMidiReceiver : MidiReceiver() {
    override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
        // Здесь вы обрабатываете входящий MIDI-пакет (массив байт)
    }
}

// 2. Находим порт, который является выходом для MIDI-устройства
val portInfo = midiDevice.info.ports.first { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }

// 3. Открываем этот порт как выходной для нашего приложения и подключаем обработчик
val outputPort = midiDevice.openOutputPort(portInfo.portNumber)
outputPort.connect(MyMidiReceiver())
```

### 8. Отправка данных на устройство

Для отправки данных открывается **входной порт**. В него можно напрямую записывать байтовые массивы с MIDI-командами.

```kotlin
// 1. Находим порт, который является входом для MIDI-устройства
val portInfo = midiDevice.info.ports.first { it.type == MidiDeviceInfo.PortInfo.TYPE_INPUT }

// 2. Открываем этот порт как входной для нашего приложения
val inputPort = midiDevice.openInputPort(portInfo.portNumber)
val buffer = ByteArray(32)
// ...заполняем buffer MIDI-командой...
inputPort.send(buffer, 0, buffer.size)
```

Не забудьте закрывать порты (`port.close()`) и устройства (`device.close()`), когда они больше не нужны.

## Структура MIDI-сообщений

Данные от MIDI-устройства приходят в виде массива байтов (`ByteArray`). Чтобы понять, что означают эти байты, нужна спецификация протокола MIDI 1.0.

Рассмотрим пример функции для парсинга входящих данных:

```kotlin
fun parse(data: ByteArray): Note? {
    if (data.isEmpty()) return null

    // Статус-байт MIDI-сообщения (первый байт)
    val status = data[0].toInt() and 0xFF
    // Команда сообщения (старшие 4 бита)
    val command = status and 0xF0
    // MIDI-канал (младшие 4 бита)
    val channel = status and 0x0F

    // 0x90 - команда Note On
    // Мы проверяем только команду, игнорируя канал
    if (command == 0x90) {
        // Убедимся, что в сообщении достаточно данных
        if (data.size < 3) return null

        val pitch = data[1].toInt()    // Номер ноты (0-127)
        val velocity = data[2].toInt() // Сила нажатия (1-127)

        // Если velocity == 0, это эквивалент Note Off
        return if (velocity > 0) {
            Note(pitch = pitch, velocity = velocity, channel = channel)
        } else {
            // Это сообщение Note Off, возвращаем null или специальный объект
            null
        }
    }

    // Здесь можно добавить обработку других команд (Note Off 0x80, Control Change 0xB0 и т.д.)

    return null
}
```

Разбор байтов с опорой на спецификацию:

*   `val status = data[0].toInt() and 0xFF`: Вы берете первый байт. Согласно спецификации, это **Status Byte**.
*   `val command = status and 0xF0`: Вы используете маску `0xF0`, чтобы отделить старшие 4 бита, которые определяют команду. В таблице спецификации это соответствует столбцу "Code (Binary)". Например, для *Note On* это `1001nnnn`. Ваша маска `0xF0` (в двоичном виде `11110000`) оставляет только `10010000`, что равно `0x90`.
*   `if (command == 0x90)`: Вы сравниваете результат с `0x90`. Согласно таблице, это статус-байт для команды **Note On**.
*   `val pitch = data[1].toInt()` и `val velocity = data[2].toInt()`: Вы читаете второй и третий байты. Спецификация подтверждает, что за статус-байтом *Note On* следуют два байта данных: "Byte 2" (**Key number**) и "Byte 3" (**Velocity**).

### Что такое MIDI-канал (Channel)

В коде выше мы также извлекаем номер канала: `val channel = status and 0x0F`. Что это такое?

Представьте, что у вас есть оркестр, но только один дирижер (одно MIDI-соединение). MIDI-каналы — это способ дирижера обращаться к конкретной группе музыкантов (например, "только скрипки" или "только барабаны").

*   **Всего 16 каналов:** Протокол MIDI 1.0 определяет 16 независимых каналов. В коде они нумеруются **от 0 до 15**, но для пользователей (например, в настройках синтезаторов) они обычно отображаются как каналы **с 1 по 16**.

*   **Как это работает:** Большинство MIDI-сообщений (которые называются "канальными" — Channel Messages) в своем первом байте (статус-байте) содержат 4-битный номер канала.
    *   Статус-байт `1001nnnn` (Note On) или `1000nnnn` (Note Off).
    *   Старшие 4 бита (`1001`) определяют саму команду.
    *   Младшие 4 бита (`nnnn`) — это и есть номер канала (значение от `0000` до `1111`, т.е. от 0 до 15).
    *   Код `status and 0x0F` как раз и выделяет эти 4 бита.

*   **Зачем это нужно:** Каналы позволяют по одному MIDI-кабелю управлять сразу 16-ю разными инструментами (тембрами). Например, MIDI-клавиатура может отправлять ноты для пианино по каналу 1, а для баса — по каналу 2. Синтезатор, принимающий эти данные, будет знать, какой звук воспроизводить для каждой ноты, основываясь на номере ее канала. Канал 10 традиционно зарезервирован для ударных инструментов.

Таким образом, информация о том, как устроен массив `data`, поступающий в вашу функцию, напрямую происходит из официальных спецификаций, опубликованных на сайте [midi.org](https://www.midi.org/).

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

### Спецификация MIDI

*   **Спецификация сообщений MIDI 1.0**:
    *   https://midi.org/spec-detail

### Android MIDI API

*   **Обзор пакета android.media.midi**:
    *   [https://developer.android.com/reference/android/media/midi/package-summary](https://developer.android.com/reference/android/media/midi/package-summary)
*   **Официальная документация по MidiManager**:
    *   [https://developer.android.com/reference/android/media/midi/MidiManager](https://developer.android.com/reference/android/media/midi/MidiManager)
*   **Гайд по Native MIDI API (для C/C++)**:
    *   [https://developer.android.com/ndk/guides/audio/midi](https://developer.android.com/ndk/guides/audio/midi)
*   **Официальные примеры (Best Practices)**:
    *   [https://github.com/android/midi-samples](https://github.com/android/midi-samples)
