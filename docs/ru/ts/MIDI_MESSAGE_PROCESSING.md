# Техническое задание: Реализация обработки и отображения MIDI-сообщений

## 1. Общая информация

### 1.1. Цель доработки

Реализовать функционал для приема, обработки и визуализации MIDI-сообщений (нот), поступающих от подключенной MIDI-клавиатуры. Основная задача — отображать сыгранные пользователем ноты и аккорды на нотном стане в реальном времени.

### 1.2. Базовые документы

- [Архитектурные принципы](../plans/ARCHITECTURE_PRINCIPLES.md)
- [Сценарии: Прием и отображение MIDI-сообщений](../uc/MIDI_MESSAGE_PROCESSING.md)
- [Техническое задание: Реализация отслеживания состояния подключения MIDI-клавиатуры](./MIDI_CONNECTION.md)

## 2. Архитектурное решение

### 2.1. Компоненты

Система обработки MIDI-сообщений интегрирована в существующую архитектуру, расширяя функционал `Data` и `Domain` слоев и добавляя новые компоненты в `Presentation` слой.

**Data Layer**
- **`MidiDataSource`:** Расширен для обработки входящих MIDI-сообщений. После успешного открытия устройства (`MidiManager.openDevice`), он подключает `MidiReceiver` к **выходному порту** устройства (`MidiOutputPort`) для приема данных.
- **`MidiMessageReceiver`:** Внутренняя реализация `android.media.midi.MidiReceiver`, ответственная за прием сырых MIDI-данных (`byte[]`).
- **`MidiMessageParser`:** Компонент, который получает сырые данные от `MidiMessageReceiver`, парсит их и преобразует в доменную модель `Note`. Игнорирует все сообщения, кроме `Note On`.
- **`MidiRepositoryImpl`:** Реализация репозитория, которая предоставляет поток входящих нот.

**Domain Layer**
- **`Note`:** Доменная модель, представляющая одну ноту (высота тона).
- **`MidiRepository`:** Интерфейс дополнен методом для наблюдения за входящими нотами (`Flow<Note>`).
- **`ObserveMidiMessagesUseCase`:** `Use Case`, который получает поток одиночных нот и преобразует его в `Flow<List<Note>>`, группируя быстрые последовательные нажатия в один аккорд.

**Presentation Layer**
- **`PianoStaffViewModel`:** Новая `ViewModel` для экрана с нотным станом. Она получает `Flow` нот из `ObserveMidiMessagesUseCase` и преобразует его в состояние для UI.
- **`PianoStaffScreen`:** `Composable`-экран, который отображает сыгранные ноты на нотном стане.
- **`MainActivity`**: Основная `Activity` приложения. Использует `setContent` для отображения `Composable`-иерархии, построенной на `Scaffold`, который управляет структурой экрана и содержит `SnackbarHost` для показа уведомлений.

```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Component.puml

title C4 - Level 3: Компоненты системы обработки MIDI-сообщений

System_Ext(midi_device, "MIDI Keyboard", "Физическое устройство")
System_Ext(android_sdk, "Android SDK", "MidiReceiver, MidiOutputPort")

Container_Boundary(presentation, "Presentation Layer") {
    Component(activity, "MainActivity", "Activity", "Отображает Composable UI с помощью Scaffold.")
    Component(vm, "PianoStaffViewModel", "ViewModel", "Управляет состоянием UI, получая ноты и формируя PianoStaffUiState.")
    Component(screen, "PianoStaffScreen", "Composable", "Экран, который отображает нотный стан, получая состояние от ViewModel.")
    Component(staff, "PianoStaff", "Composable", "Компонент для непосредственной отрисовки нотного стана.")
}

Container_Boundary(domain, "Domain Layer") {
    Component(observe_uc, "ObserveMidiMessagesUseCase", "Use Case", "Группирует ноты в аккорды.")
    Component(repo, "MidiRepository", "Interface", "Контракт для получения MIDI-данных.")
    Component(note, "Note", "Data Class", "Доменная модель ноты.")
}

Container_Boundary(data, "Data Layer") {
    Component(repo_impl, "MidiRepositoryImpl", "Implementation", "Реализация репозитория.")
    Component(ds, "MidiDataSource", "Data Source", "Получает сообщения через MidiReceiver.")
    Component(parser, "MidiMessageParser", "Parser", "Парсит сырые MIDI-сообщения.")
    Component(receiver, "MidiMessageReceiver", "Receiver", "Реализация android.media.midi.MidiReceiver.")
}

' Связи
Rel(activity, screen, "Отображает")
Rel(screen, staff, "Использует")
Rel(screen, vm, "Наблюдает за", "PianoStaffUiState")
Rel(vm, observe_uc, "Вызывает")
Rel(observe_uc, repo, "Вызывает observeNotes()")

Rel(repo_impl, repo, "@Binds")
Rel(repo_impl, ds, "Зависит от")

Rel(ds, parser, "Использует")
Rel(ds, receiver, "Создает и подключает")
Rel(receiver, android_sdk, "Реализует")
Rel(receiver, parser, "Передает данные в")

Rel(midi_device, ds, "Отправляет MIDI-сообщения", "USB/Bluetooth")

Rel(observe_uc, note, "Возвращает Flow<List<Note>>")
Rel(parser, note, "Создает")

@enduml
```

### 2.2. API и Модели данных

**Domain Layer:**

```kotlin
// com.astrizhachuk.pianoflow.domain.model.Note.kt
data class Note(
    val pitch: Int // MIDI номер ноты (0-127)
)

// com.astrizhachuk.pianoflow.domain.repository.MidiRepository.kt
interface MidiRepository {
    fun observeConnectionState(): Flow<ConnectionState>
    fun observeNotes(): Flow<Note> // Возвращает поток одиночных нот
}
```

**Presentation Layer:**

```kotlin
// com.astrizhachuk.pianoflow.presentation.model.pianostaff.PianoStaffUiState.kt
data class PianoStaffUiState(
    val notesJson: String = "{"treble":[], "bass":[]}"
)

// com.astrizhachuk.pianoflow.presentation.viewmodel.pianostaff.PianoStaffViewModel.kt
@HiltViewModel
class PianoStaffViewModel @Inject constructor(
    private val observeMidiMessagesUseCase: ObserveMidiMessagesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PianoStaffUiState())
    val uiState: StateFlow<PianoStaffUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeMidiMessagesUseCase().collect { notes ->
                _uiState.update {
                    it.copy(notesJson = notes.toVexflowJson())
                }
            }
        }
    }
}
```

**PianoStaffScreen** — это `Composable`-функция, которая получает `PianoStaffViewModel` через Hilt, подписывается на изменения `uiState` и передает данные в `PianoStaff` для отрисовки.

```kotlin
// com.astrizhachuk.pianoflow.presentation.ui.pianostaff.PianoStaffScreen.kt
@Composable
fun PianoStaffScreen(
    modifier: Modifier = Modifier,
    viewModel: PianoStaffViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        PianoStaff(notesJson = uiState.notesJson)
    }
}
```

### 2.3. Расширение зависимостей

`MidiMessageParser` был добавлен в граф зависимостей Hilt и внедрен в `MidiDataSource`.

```kotlin
// com.astrizhachuk.pianoflow.data.di.DataModule.kt
// ...
companion object {
    @Provides
    @Singleton
    fun provideMidiDataSource(
        @ApplicationContext context: Context,
        midiDeviceMapper: MidiDeviceMapper,
        midiMessageParser: MidiMessageParser // Добавлена зависимость
    ): MidiDataSource {
        return MidiDataSource(context, midiDeviceMapper, midiMessageParser)
    }

    @Provides
    @Singleton
    fun provideMidiMessageParser(): MidiMessageParser {
        return MidiMessageParser()
    }
}
```

```plantuml
@startuml
title Внедрение зависимости MidiMessageParser через Hilt

class MidiMessageParser <<@Singleton>>
class MidiDataSource <<@Singleton>>

abstract class DataModule <<Hilt Module>> {
  +provideMidiMessageParser(): MidiMessageParser
  +provideMidiDataSource(...,
  midiMessageParser: MidiMessageParser): MidiDataSource
}

note right of DataModule::provideMidiMessageParser
  Этот метод сообщает Hilt,
  как создавать MidiMessageParser.
end note

' Зависимости
DataModule::provideMidiMessageParser ..> MidiMessageParser : <<@Provides>>
DataModule::provideMidiDataSource ..> MidiDataSource : <<@Provides>>
MidiDataSource --> MidiMessageParser : <<inject>>

@enduml
```

## 3. Жизненный цикл и взаимодействие

### 3.1. Принцип работы

1.  **Подключение `Receiver`'а**:
    *   После того как `MidiDataSource` успешно открывает соединение с MIDI-устройством, он находит первый доступный **выходной порт** (`MidiOutputPort`) устройства.
    *   `MidiDataSource` вызывает `outputPort.connect(midiMessageReceiver)`, чтобы начать получать MIDI-данные.

```plantuml
@startuml
title Диаграмма последовательности: Подключение MidiMessageReceiver

participant "Android MIDI System" as MidiSystem
box "Приложение PianoFlow" #LightGray
    participant "MidiDataSource" as DS
end box

note over MidiSystem, DS
  Процесс инициируется после успешного
  асинхронного открытия устройства (см. [[./MIDI_CONNECTION.md ТЗ по MIDI Connection]])
end note

MidiSystem -> DS: onDeviceOpened(device)
activate DS

DS -> MidiSystem: device.openOutputPort(portNumber)
note right: Находит и открывает выходной порт MIDI-устройства

MidiSystem --> DS: outputPort

DS -> MidiSystem: outputPort.connect(midiMessageReceiver)
note right: Подключает `Receiver` к порту для прослушивания

deactivate DS
@enduml
```

2.  **Прием и парсинг сообщений**:
    *   Когда пользователь нажимает клавишу, MIDI-клавиатура отправляет сообщение. `MidiMessageReceiver.onSend()` вызывается с сырыми данными (`byte[]`).
    *   `MidiMessageReceiver` немедленно передает эти данные в `MidiMessageParser`.
    *   `MidiMessageParser` анализирует байты. Если это `Note On`, он извлекает номер ноты и создает объект `Note`, который передает обратно в `MidiDataSource`.
    *   `MidiDataSource` отправляет полученную `Note` в `SharedFlow`.

```plantuml
@startuml
title Диаграмма последовательности: Внутренняя работа midiMessageReceiver

participant "Android MIDI System" as MidiSystem
box "MidiDataSource" #LightGray
    participant "midiMessageReceiver" as Receiver
    participant "midiMessageParser" as Parser
    participant "_notes: MutableSharedFlow" as NotesFlow
end box
participant "Timber" as Logger

MidiSystem -> Receiver : onSend(msg, offset, count, ...)
activate Receiver

Receiver -> Receiver : relevantData = msg.copyOfRange(...)
note right: Извлечение байтов текущего сообщения

Receiver -> Parser : parse(relevantData)
activate Parser
Parser --> Receiver : note
deactivate Parser

Receiver -> NotesFlow : tryEmit(note)
activate NotesFlow

alt Успешная отправка
    NotesFlow --> Receiver : true
else Буфер переполнен
    NotesFlow --> Receiver : false
    Receiver -> Logger : w("Failed to emit note...")
end

deactivate NotesFlow
deactivate Receiver

@enduml
```

3.  **Группировка и передача нот**:
    *   `MidiRepositoryImpl` через `observeNotes()` проксирует `Flow<Note>` из `MidiDataSource`.
    *   `ObserveMidiMessagesUseCase` подписывается на этот поток. Он использует операторы `Kotlin Flow` (например, `channelFlow` с `delay`) для группировки нот, пришедших в течение короткого промежутка времени (`50 мс`), в один список `List<Note>` (аккорд).

4.  **Отображение на UI**:
    *   `PianoStaffViewModel` подписывается на `Flow<List<Note>>` от `ObserveMidiMessagesUseCase`.
    *   `PianoStaffViewModel` преобразует список нот в JSON-строку с помощью `toVexflowJson()` и обновляет свой `StateFlow<PianoStaffUiState>`, который содержит JSON-строку `notesJson`.
    *   `PianoStaffScreen`, подписанный на `uiState`, получает эту JSON-строку и передает ее в `Composable`-компонент `PianoStaff` для финальной отрисовки.

```plantuml
@startuml
title Диаграмма последовательности: Обработка Note On

actor Пользователь as User
participant "MIDI-клавиатура" as Keyboard
box "Приложение PianoFlow"
  participant "MidiDataSource" as DS
  participant "ObserveMidiMessagesUseCase" as UC
  participant "PianoStaffViewModel" as VM
  participant "PianoStaffScreen" as Screen
  participant "PianoStaff" as Staff
end box

User -> Keyboard : Нажимает клавишу(и)
Keyboard -> DS : Отправляет MIDI-сообщение
activate DS

note right of DS
  Прием и парсинг сообщения.
  Подробности см. в диаграмме
  "Внутренняя работа midiMessageReceiver".
end note

DS -> UC : Отправляет новую ноту в Flow<Note>
deactivate DS
activate UC

note right of UC: Группирует ноты в список (аккорд)
UC -> VM : Отправляет Flow<List<Note>>
deactivate UC
activate VM

VM -> VM : Преобразует списки нот в JSON
VM -> VM : Обновляет uiState
VM -> Screen : Передает новое состояние (UiState с JSON)
deactivate VM
activate Screen

Screen -> Staff : Передает JSON с нотами
activate Staff
Staff -> User : Отображает ноты на нотном стане
deactivate Staff
deactivate Screen

@enduml
```

### 3.2. Механизм отображения нот

Отрисовка нотного стана выполняется с помощью `WebView` и JavaScript-библиотеки [VexFlow](https://www.vexflow.com/). Этот подход позволяет отделить логику отрисовки от нативного кода, используя мощные возможности веб-технологий для визуализации музыкальной нотации.

**Ключевые компоненты:**

- **`PianoStaff` Composable:** Оборачивает `AndroidView`, в котором создается и настраивается `WebView`. Этот компонент отвечает за управление жизненным циклом `WebView` и его перерисовку.
- **`VexflowNoteMapper.kt`**: Содержит логику преобразования `List<Note>` в конечный JSON-объект. Эта логика включает разделение нот по нотоносцам и создание "фантомных" нот для удобочитаемости.
- **`vexflow.html`:** HTML-файл, расположенный в `app/src/main/assets/`. Он содержит базовую разметку, стили и основной JavaScript-код для работы с VexFlow.
- **`vexflow.js`:** Сама библиотека VexFlow (версия [4.2.2](https://cdn.jsdelivr.net/npm/vexflow@4.2.2/build/cjs/vexflow.js)), также расположенная в `assets`.

**Процесс работы:**

1.  **Инициализация `WebView`:** `PianoStaff` создает и запоминает (`remember`) экземпляр `WebView`. Устанавливается `WebViewClient`, который отслеживает окончание загрузки страницы.
2.  **Загрузка страницы:** `WebView` загружает `vexflow.html`. Когда загрузка завершена, `onPageFinished` обновляет флаг состояния `isPageLoaded`.
3.  **Отслеживание размера:** Модификатор `onSizeChanged` отслеживает изменения размера `Composable`-компонента и обновляет состояние `viewSize`.
4.  **Передача данных и отрисовка:** `LaunchedEffect` следит за изменением любого из трех состояний: `notesJson`, `viewSize` или `isPageLoaded`. Когда все условия выполнены (страница загружена, размер известен, и пришли новые ноты), `LaunchedEffect` выполняет следующие действия:
    *   Определяет ориентацию (`landscape` или `portrait`) на основе размеров `viewSize`.
    *   Формирует и выполняет JavaScript-код, который сначала очищает предыдущий SVG-элемент, а затем вызывает функцию `drawGrandStaff` с данными о нотах и ориентации.
5.  **Формат JSON:** Данные передаются в виде единого JSON-объекта, который содержит отдельные массивы для скрипичного и басового ключей.
    ```json
    {
      "treble": [{"keys":["c/5"], "duration":"w"}, {"keys":["g/4"], "duration":"w", "ghost":true}],
      "bass": [{"keys":["c/4"], "duration":"w"}, {"keys":["e/3", "g/3"], "duration":"w"}]
    }
    ```

**Диаграмма состояний жизненного цикла `PianoStaff`**

Эта диаграмма показывает, как `PianoStaff` управляет своим состоянием для корректной отрисовки.

```plantuml
@startuml
title Диаграмма состояний PianoStaff

[*] --> Initializing : Composable входит в композицию

state Initializing
Initializing : Создается WebView
Initializing : isPageLoaded = false
Initializing : viewSize = IntSize.Zero

Initializing --> LoadingPage : webView.loadUrl(...)

state LoadingPage
LoadingPage : Ожидание onPageFinished

LoadingPage --> WaitingForSize : onPageFinished()
note on link
  isPageLoaded = true
end note

state WaitingForSize
WaitingForSize : // Это промежуточное состояние, ожидающее размера

WaitingForSize --> ReadyToDraw : onSizeChanged(size) [size != IntSize.Zero]
note on link
  viewSize обновлен
end note

state ReadyToDraw
ReadyToDraw : isPageLoaded = true
ReadyToDraw : viewSize известен

state Drawing
Drawing : Выполняется evaluateJavascript(...)

ReadyToDraw --> Drawing : notesJson изменился
Drawing --> ReadyToDraw : Отрисовка завершена

ReadyToDraw --> Drawing : viewSize изменился
note on link
  Перерисовка с новым размером
end note

@enduml
```

**Диаграмма взаимодействия**

```plantuml
@startuml
title Диаграмма взаимодействия: Отображение нот через WebView

box "Presentation Layer (Kotlin/Compose)" #LightBlue
    participant "PianoStaffViewModel" as VM
    participant "PianoStaffScreen" as Screen
    participant "PianoStaff" as StaffComposable
end box

box "WebView (Java/Android)" #LightGreen
    participant "AndroidView" as AndroidView
    participant "WebView" as WebView
    participant "WebViewClient" as WebViewClient
end box

box "VexFlow (HTML/JavaScript)" #LightYellow
    participant "vexflow.html" as HtmlPage
    participant "drawGrandStaff()" as DrawJsFunc
end box

VM -> Screen : Обновляет UiState (с notesJson)
activate Screen

Screen -> StaffComposable : Передает новый notesJson
activate StaffComposable

alt Первый запуск
    StaffComposable -> WebView : create()
    WebView -> WebViewClient : set
    WebView -> HtmlPage : loadUrl(...)
    HtmlPage -> WebViewClient : onPageFinished()
    WebViewClient -> StaffComposable : isPageLoaded = true
end

alt notesJson, viewSize или isPageLoaded изменились
    StaffComposable -> StaffComposable : LaunchedEffect
    StaffComposable -> WebView : evaluateJavascript("drawGrandStaff(...)")
    activate WebView
    WebView -> HtmlPage : Вызывает JS-функцию
    activate HtmlPage
    HtmlPage -> DrawJsFunc : drawGrandStaff(treble, bass, orientation)
    deactivate HtmlPage
    deactivate WebView
end
deactivate StaffComposable
deactivate Screen


@enduml
```

## 4. Критерии приемки

- При нажатии одной клавиши на MIDI-клавиатуре соответствующая нота немедленно отображается на нотном стане.
- При одновременном нажатии нескольких клавиш (аккорд) все соответствующие ноты отображаются на нотном стане.
- Каждое новое событие нажатия (`Note On`) приводит к полной очистке экрана перед отображением новых нот.
- Система реагирует только на сообщения о нажатии клавиш (`Note On`); сообщения об отпускании (`Note Off`) и другие типы MIDI-сообщений игнорируются.
- Визуализация нот на экране происходит без видимых задержек после нажатия клавиши.
- Функционал стабильно работает при быстром и многократном нажатии клавиш, приложение не падает и не зависает.
- При повороте экрана нотный стан корректно перерисовывается с учетом новой ориентации.

## См. также

- [См. документ о Kotlin Flow](../tech/KOTLIN_FLOW.md)
- [См. документ о MIDI API в Android](../tech/MIDI.md)
