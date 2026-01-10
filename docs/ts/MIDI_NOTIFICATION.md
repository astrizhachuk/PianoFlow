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
    val userMessages: Flow<UserMessage>
    fun showMessage(message: UserMessage)
}
```

### 2.3. Внедрение зависимостей

Чтобы Hilt знал, что при запросе интерфейса `UserNotifier` нужно предоставлять экземпляр класса `UserNotifierImpl`, используется **@Module**.

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
  - _userMessages: MutableSharedFlow<UserMessage>
  + userMessages: Flow<UserMessage>
  + showMessage(message: UserMessage)
}

note right of UserNotifierImpl::showMessage
  Вызов этого метода отправляет
  сообщение в приватный поток `_userMessages`.
end note

note right of UserNotifierImpl::userMessages
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
3.  **Подписчик** (`Activity`) при своем создании подписывается на поток сообщений `userMessages` из `UserNotifier`.
4.  Как только издатель отправляет новое сообщение, `Activity` немедленно его получает и отображает на экране (например, в виде `Toast`).

### 3.2. Последовательность событий

Диаграмма ниже иллюстрирует типичный сценарий: `MidiConnectionViewModel` (издатель) отправляет уведомление о подключении устройства, а `Activity` (подписчик) его отображает.

```plantuml
@startuml
title Сценарий: Отображение уведомления

participant "Activity" as UI
participant "UserNotifier" as Notifier
participant "MidiConnectionViewModel" as VM

note over Notifier: Singleton, живет с приложением

activate UI
note left of UI : Hilt внедряет `UserNotifier`
UI -> UI : onCreate()
UI -> UI : observeNotifications()
UI -> Notifier : userMessages.collect()
note right of UI : Activity начинает слушать\nсообщения

... Спустя какое-то время ...

== Происходит событие (например, подключение MIDI) ==

UI -> VM : <<create>> первое обращение
activate VM
note over VM
 Hilt внедряет тот же `UserNotifier`.
 Выполняется `init` блок и внутренняя логика.
 VM решает показать сообщение.
end note
VM -> Notifier : showMessage(UserMessage)
activate Notifier
Notifier -> Notifier : Помещает сообщение в SharedFlow
Notifier -> UI : Отправляет сообщение подписчику
deactivate Notifier
deactivate VM

UI -> UI : Отображает Toast
deactivate UI
@enduml
```

### 3.3. Управление жизненным циклом

Правильное управление жизненным циклом гарантирует, что система работает эффективно и без утечек памяти.

*   **Издатель (`MidiConnectionViewModel`)**: Его жизненный цикл привязан к `Activity`. Он может отправлять сообщения в `UserNotifier` в любой момент, пока существует.
*   **Шина событий (`UserNotifier`)**: Так как это **синглтон** (`@Singleton`), он живет на протяжении всего жизненного цикла приложения. Это гарантирует, что он всегда доступен для отправки и получения сообщений, выступая стабильным посредником.
*   **Подписчик (`Activity`)**: Подписка на сообщения происходит внутри `lifecycleScope`. Это означает, что как только `Activity` будет уничтожена, подписка автоматически отменится. Это ключевой момент, который предотвращает попытки обновить уже не существующий UI и защищает от утечек памяти.

Таким образом, `ViewModel` ничего не знает о `View`, а `View` безопасно подписывается на события, жизненный цикл которых управляется автоматически.

## 4. Критерии приемки

- ✅ При подключении MIDI-клавиатуры на экране появляется Toast с сообщением, содержащим ее имя.
- ✅ При отключении MIDI-клавиатуры на экране появляется Toast с сообщением "MIDI-клавиатура отключена".
- ✅ Принудительной инициализации `MidiConnectionViewModel` в `Activity` (например, в MainActivity) гарантирует, что отслеживание подключения запускается при старте приложения.
