# Техническое задание: Реализация механизма уведомлений пользователя

## 1. Общая информация

### 1.1. Цель доработки

Реализовать универсальный, переиспользуемый компонент для асинхронной отправки и отображения уведомлений пользователю (например, Toast/Snackbar).

### 1.2. Базовые документы

- [Архитектурные принципы](../plans/ARCHITECTURE_PRINCIPLES.md)
- [Сценарии: Отслеживание состояния и уведомления](../uc/MIDI_KEYBOARD_CONNECTION_STATE.md)
- [Техническое задание: Отслеживание подключения USB MIDI-клавиатуры](./MIDI_CONNECTION.md)

## 2. Архитектурное решение

### 2.1. Компоненты

Для уведомления пользователя в приложении вводится механизм, состоящий из следующих компонентов в `Presentation Layer`:

*   `UserNotifier` (интерфейс) и `UserNotifierImpl` (реализация): Обеспечивают асинхронную передачу UI-событий от `ViewModel` к `View` (`Activity`/`Fragment`) по принципу "издатель-подписчик".
*   `UserMessage`: Модель данных для сообщения, отображаемого пользователю.
*   `NotificationModule`: Hilt-модуль, который предоставляет зависимость `UserNotifier` для использования в других компонентах.

Этот механизм позволяет `ViewModel` отправлять сообщения, не имея прямой ссылки на `View`, что соответствует принципам чистой архитектуры.

```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Component.puml

title C4 - Уровень 3: Компоненты механизма уведомлений

Container_Boundary(presentation, "Presentation Layer") {
    Component(vm, "MidiConnectionViewModel", "ViewModel", "Формирует и отправляет сообщение.")
    Component(ui, "Activity", "UI-компонент, например, MainActivity", "Отображает сообщение.")

    Component(notifier, "UserNotifier", "Интерфейс", "Контракт для отправки и получения уведомлений.")
    Component(impl, "UserNotifierImpl", "Singleton", "Реализация шины событий на SharedFlow.")
    Component(message, "UserMessage", "Data Class", "Модель данных для UI.")
    Component(di, "NotificationModule", "Hilt Module", "Предоставляет зависимость UserNotifier.")

    Rel(impl, notifier, "Реализует")
    Rel(di, notifier, "@Binds")

    Rel(vm, notifier, "Использует для отправки")
    Rel(ui, notifier, "Использует для подписки")
    
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

class UserNotifier <<interface>>
class UserNotifierImpl <<@Singleton>>

class NotificationModule <<Hilt Module>> {
  +bindUserNotifier(impl: UserNotifierImpl): UserNotifier
}

UserNotifierImpl .up.|> UserNotifier : реализует
NotificationModule ..> UserNotifierImpl : предоставляет
NotificationModule::bindUserNotifier ..> UserNotifier : "(@Binds)"
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

Ключевая задача системы уведомлений — отделить компоненты, которые *создают* сообщения (издатели), от компонентов, которые их *отображают* (подписчики). Это достигается через центральную шину событий — `UserNotifier`.

### 3.1. Принцип работы

Взаимодействие строится по принципу "издатель-подписчик":

1.  **Издатель** (например, `MidiConnectionViewModel`) на основе своей внутренней логики решает, что нужно уведомить пользователя. Например, изменилось состояние подключения MIDI-клавиатуры.
2.  Он создает объект `UserMessage` и отправляет его в `UserNotifier`, который является синглтоном и доступен во всем приложении.
3.  **Подписчик** (`Activity`) при своем создании подписывается на поток сообщений `messages` из `UserNotifier`.
4.  Как только издатель отправляет новое сообщение, `Activity` немедленно его получает и отображает на экране (например, в виде `Toast`).

### 3.2. Последовательность событий

Диаграмма ниже иллюстрирует типичный сценарий с учетом `repeatOnLifecycle`.

```plantuml
@startuml
title Сценарий: Отображение уведомления (финальная версия)

participant "Activity" as UI
participant "UserNotifier (Singleton)" as Notifier
participant "ViewModel" as VM

activate Notifier #Gainsboro
note over Notifier: Singleton, существует в течение всего\nжизненного цикла приложения.

activate UI
UI -> UI : onCreate()

== UI переходит в состояние STARTED ==
UI -> Notifier : подписывается на messages.collect()
activate UI #LightBlue
note right of UI: **Корутина-сборщик** запущена.\nОна будет активна, пока UI > STARTED.

loop пока UI в состоянии STARTED

    note over UI, VM : Происходит событие, и VM отправляет уведомление
    activate VM
    VM -> Notifier : sendMessage(message)
    deactivate VM

    Notifier -> UI : (message)
    note right of UI: Корутина возобновляется (resume),\nобрабатывает сообщение...
    UI -> UI: Отображает Toast
    note right of UI: ...и **снова приостанавливается** (suspend)\nв ожидании следующего сообщения.

end

== UI переходит в состояние STOPPED ==
note right of UI: `repeatOnLifecycle` отменяет\nкорутину-сборщик.
deactivate UI #LightBlue

== UI переходит в состояние DESTROYED ==
deactivate UI
deactivate Notifier
@enduml
```

### 3.3. Управление жизненным циклом

Правильное управление жизненным циклом подписки — ключ к эффективной и безопасной работе с UI. С добавлением `repeatOnLifecycle` он становится строго детерминированным.

*   **Издатель (`MidiConnectionViewModel`)**: Его жизненный цикл привязан к графу навигации или `Activity`. Он может отправлять сообщения в `UserNotifier` в любой момент, пока существует.
*   **Шина событий (`UserNotifier`)**: Является синглтоном (`@Singleton`), поэтому живет на протяжении всего жизненного цикла приложения. Это гарантирует, что он всегда доступен для отправки и получения сообщений.
*   **Подписчик (`Activity`)**: Подписка на сообщения (`messages.collect`) выполняется внутри блока `repeatOnLifecycle(Lifecycle.State.STARTED)`. Это определяет точный и однозначный жизненный цикл подписки:
    *   **Подписка создается и становится активной**: когда `Activity` переходит в состояние `STARTED` (сразу после вызова `onStart()`). Именно в этот момент `Activity` начинает слушать и обрабатывать сообщения.
    *   **Подписка умирает (отменяется)**: когда `Activity` уходит с экрана и переходит в состояние `STOPPED` (сразу после вызова `onStop()`). Сбор сообщений полностью прекращается.

Диаграмма состояний ниже наглядно иллюстрирует жизненный цикл подписки в `Activity`. 

```plantuml
@startuml
title Жизненный цикл подписки в Activity
hide empty description

state "Подписка неактивна" as Inactive
state "Подписка активна" as Active

[*] --> Inactive : onCreate
Inactive --> Active : onStart
Active --> Inactive : onStop

Active --> [*] : onDestroy
Inactive --> [*] : onDestroy
@enduml
```

При возвращении пользователя в `Activity` (повторный вызов `onStart`), `repeatOnLifecycle` запускает блок с подпиской **заново**. Окончательно вся корутина, запущенная в `lifecycleScope`, отменяется при уничтожении `Activity` (после вызова `onDestroy`).

Таким образом, мы имеем гарантию, что UI обновляется только тогда, когда он виден пользователю, что предотвращает бесполезную работу в фоновом режиме и ошибки, связанные с обновлением невидимого интерфейса.

## 4. Критерии приемки

- ✅ При подключении MIDI-клавиатуры на экране появляется Toast с сообщением, содержащим ее имя.
- ✅ При отключении MIDI-клавиатуры на экране появляется Toast с сообщением "MIDI-клавиатура отключена".

## См. также

[См. документ о Kotlin Flow](../tech/KOTLIN_FLOW.md)
