# Техническое задание: Реализация механизма уведомлений пользователя

## 1. Общая информация

### 1.1. Цель доработки

Реализовать универсальный, переиспользуемый компонент для асинхронной отправки и отображения уведомлений пользователю с помощью Jetpack Compose.

### 1.2. Базовые документы

- [Архитектурные принципы](../plans/ARCHITECTURE_PRINCIPLES.md)
- [Сценарии: Отслеживание состояния и уведомления](../uc/MIDI_KEYBOARD_CONNECTION_STATE.md)
- [Техническое задание: Отслеживание подключения USB MIDI-клавиатуры](./MIDI_CONNECTION.md)

## 2. Архитектурное решение

### 2.1. Компоненты

Для уведомления пользователя в приложении вводится механизм, состоящий из следующих компонентов в `Presentation Layer`:

*   `UserNotifier` (интерфейс) и `UserNotifierImpl` (реализация): Обеспечивают асинхронную передачу UI-событий от `ViewModel` к UI-слою по принципу "издатель-подписчик".
*   `UserMessage`: Модель данных для сообщения, отображаемого пользователю.
*   `NotificationModule`: Hilt-модуль, который предоставляет зависимость `UserNotifier`.
*   **`MainActivity`**: Выступает в роли хоста для `Composable`-компонентов, содержит `Scaffold` со `SnackbarHost`.
*   **`ObserveNotifications`**: `Composable`-функция-наблюдатель, которая подписывается на `UserNotifier` и показывает `Snackbar`.

Этот механизм позволяет `ViewModel` отправлять сообщения, не имея прямой ссылки на UI, что соответствует принципам чистой архитектуры.

```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Component.puml

title C4 - Уровень 3: Компоненты механизма уведомлений (Compose)

Container_Boundary(presentation, "Presentation Layer") {
    Component(vm, "MidiConnectionViewModel", "ViewModel", "Формирует и отправляет сообщение.")
    Component(observer, "ObserveNotifications", "Composable", "Подписывается на UserNotifier и показывает Snackbar.")

    Component(notifier, "UserNotifier", "Interface", "Контракт для отправки и получения уведомлений.")
    Component(impl, "UserNotifierImpl", "Singleton", "Реализация шины событий на SharedFlow.")
    Component(message, "UserMessage", "Data Class", "Модель данных для UI.")
    Component(di, "NotificationModule", "Hilt Module", "Предоставляет зависимость UserNotifier.")

    Rel(impl, notifier, "Реализует")
    Rel(di, notifier, "@Binds")

    Rel(vm, notifier, "Использует для отправки")
    Rel(observer, notifier, "Использует для подписки")
    
    Rel(vm, message, "Создает")
    Rel(notifier, message, "Передает")
}
@enduml
```

### 2.2. API

```kotlin
// Модель для сообщения пользователю
data class UserMessage(val text: String)

// Интерфейс для отправки и получения сообщений
interface UserNotifier {
    val messages: Flow<UserMessage>
    fun sendMessage(message: UserMessage)
}
```

### 2.3. Внедрение зависимостей

Чтобы Hilt знал, что при запросе интерфейса `UserNotifier` нужно предоставлять экземпляр класса `UserNotifierImpl`, используется `NotificationModule`.

```kotlin
// Файл: di/NotificationModule.kt
@Module
@InstallIn(SingletonComponent::class)
interface NotificationModule {

    @Binds
    fun bindUserNotifier(impl: UserNotifierImpl): UserNotifier
}

// Файл: service/UserNotifierImpl.kt
@Singleton
class UserNotifierImpl @Inject constructor() : UserNotifier {
    // ... реализация ...
}
```

*   `@Singleton` на классе `UserNotifierImpl` указывает Hilt, что этот класс должен иметь один экземпляр на все приложение.
*   `@Binds` в `NotificationModule` связывает запрос `UserNotifier` с его реализацией. Hilt автоматически применяет скоуп `@Singleton` из реализации к этому связыванию.

```plantuml
@startuml
title Связывание интерфейса и реализации через Hilt

interface UserNotifier
class UserNotifierImpl <<@Singleton>>

interface NotificationModule <<Hilt Module>> {
  +bindUserNotifier(impl: UserNotifierImpl): UserNotifier
}

' Реализация
UserNotifierImpl .up.|> UserNotifier

' Зависимость
NotificationModule ..> UserNotifierImpl
NotificationModule ..> UserNotifier : <<@Binds>>

@enduml
```

### 2.4. Реализация UserNotifierImpl

Класс `UserNotifierImpl` использует `MutableSharedFlow` для создания шины событий. Поток сконфигурирован со следующими параметрами:

*   `replay = 0`: Новые подписчики не получают предыдущие сообщения.
*   `extraBufferCapacity = 1`: Хранит одно сообщение, если подписчик не успевает его обработать.
*   `onBufferOverflow = BufferOverflow.DROP_OLDEST`: Отбрасывает самое старое сообщение при переполнении буфера.

```plantuml
@startuml
title Внутреннее устройство UserNotifierImpl

class UserNotifierImpl {
  - _messages: MutableSharedFlow<UserMessage>
  + messages: Flow<UserMessage>
  + sendMessage(message: UserMessage)
}

note right of UserNotifierImpl::sendMessage
  Вызов этого метода отправляет
  сообщение в приватный поток `_messages`.
end note

note right of UserNotifierImpl::messages
  Внешние подписчики получают
  сообщения из этой публичной,
  неизменяемой версии потока.
end note

@enduml
```

## 3. Жизненный цикл и взаимодействие компонентов

### 3.1. Принцип работы

Взаимодействие строится по принципу "издатель-подписчик" в рамках архитектуры Jetpack Compose:

1.  **Издатель** (например, `MidiConnectionViewModel`) на основе своей внутренней логики решает, что нужно уведомить пользователя.
2.  Он создает объект `UserMessage` и отправляет его в `UserNotifier`.
3.  **Подписчик** (`ObserveNotifications`) — это `Composable`-функция, размещенная внутри `Scaffold`.
4.  С помощью `LaunchedEffect` подписчик создает долгоживущую корутину, которая подписывается на `Flow` сообщений из `UserNotifier`.
5.  Как только издатель отправляет новое сообщение, `LaunchedEffect` его получает и вызывает `snackbarHostState.showSnackbar()` для отображения.

### 3.2. Последовательность событий

Диаграмма ниже иллюстрирует сценарий с использованием `LaunchedEffect`.

```plantuml
@startuml
title Сценарий: Отображение уведомления в Jetpack Compose

participant "MainActivity" as UI
participant "ObserveNotifications\n(Composable)" as Observer
participant "UserNotifier (Singleton)" as Notifier
participant "ViewModel" as VM

activate Notifier #Gainsboro

activate UI
UI -> Observer : (Composition)
activate Observer

== 'ObserveNotifications' входит в композицию ==
Observer -> Observer : Запускается LaunchedEffect
activate Observer #LightBlue
note right: **Корутина-сборщик** запущена.\nОна будет активна, пока 'ObserveNotifications'\nнаходится в композиции.

loop пока 'ObserveNotifications' в композиции

    note over Observer, VM : Происходит событие, и VM отправляет уведомление
    activate VM
    VM -> Notifier : sendMessage(message)
    deactivate VM

    Notifier -> Observer : (message)
    note right: Корутина возобновляется (resume),
    Observer -> Observer : snackbarHostState.showSnackbar()
    note right: ...и **снова приостанавливается** (suspend)

end

== 'ObserveNotifications' покидает композицию ==
note right: `LaunchedEffect` автоматически\nотменяет корутину-сборщик.
deactivate Observer #LightBlue

deactivate Observer
deactivate UI
deactivate Notifier
@enduml
```

### 3.3. Управление жизненным циклом

*   **Издатель (`MidiConnectionViewModel`)**: Его жизненный цикл привязан к графу навигации или `Activity`. Он может отправлять сообщения в `UserNotifier` в любой момент, пока существует.
*   **Шина событий (`UserNotifier`)**: Является синглтоном (`@Singleton`), поэтому живет на протяжении всего жизненного цикла приложения.
*   **Подписчик (`ObserveNotifications`)**: Жизненный цикл подписки теперь полностью управляется Jetpack Compose:
    *   **Подписка создается**: когда `ObserveNotifications` впервые добавляется в композицию (появляется на экране), `LaunchedEffect` запускает корутину, которая подписывается на `Flow` сообщений.
    *   **Подписка отменяется**: когда `ObserveNotifications` удаляется из композиции (например, при переходе на другой экран или закрытии `Activity`), `LaunchedEffect` автоматически отменяет корутину, безопасно прекращая подписку.

Диаграмма состояний ниже иллюстрирует жизненный цикл подписки в `Composable`.

```plantuml
@startuml
title Жизненный цикл подписки в Composable
hide empty description

state "Подписка неактивна" as Inactive
state "Подписка активна" as Active

[*] --> Inactive : (Composable не в композиции)
Inactive --> Active : (Composable входит в композицию)
Active --> Inactive : (Composable покидает композицию)

Active --> [*] : (Activity уничтожена)
Inactive --> [*] : (Activity уничтожена)
@enduml
```

Таким образом, мы имеем гарантию, что UI обновляется только тогда, когда он виден пользователю, при этом вся логика инкапсулирована внутри `Composable`-компонентов, что соответствует современным практикам.

## 4. Критерии приемки

- При подключении MIDI-клавиатуры на экране появляется уведомление (Snackbar) с сообщением, содержащим ее имя.
- При отключении MIDI-клавиатуры на экране появляется уведомление (Snackbar) с сообщением "MIDI-клавиатура отключена".

## См. также

- [См. документ о Kotlin Flow](../tech/KOTLIN_FLOW.md)
- [См. документ о MIDI API в Android](../tech/MIDI.md)
