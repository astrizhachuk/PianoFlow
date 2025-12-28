# Техническое задание: Реализация отслеживания подключения USB MIDI-клавиатуры

## 1. Общая информация

**Дата реализации:** 2024-12-28  
**Версия:** 1.0  
**Статус:** ✅ Реализовано и протестировано

### 1.1. Цель доработки

Реализация функционала автоматического отслеживания подключения и отключения USB MIDI-клавиатуры к Android-устройству.

### 1.2. Базовые документы

- [Техническое задание: Реализация уведомлений](./MIDI_NOTIFICATION.md) — ТЗ по уведомлениям
- [Use Cases: USB MIDI клавиатура](../uc/USB_MIDI_KEYBOARD.md) — описание функциональных требований
- [Архитектурные принципы](../plans/ARCHITECTURE_PRINCIPLES.md) — принципы проектирования
- [Описание приложения](../plans/APPLICATION_DESCRIPTION.md) — общее описание приложения

## 2. Реализованный Use Case

### 2.1. UC-001: Отслеживание подключения MIDI-клавиатуры

**Статус:** ✅ Реализовано

**Реализованные сценарии:**
- ✅ Автоматическое отслеживание подключенных MIDI-устройств при запуске приложения
- ✅ Автоматическое подключение к первому найденному устройству
- ✅ Обнаружение устройства, уже подключенного при запуске приложения
- ✅ Обработка ошибок подключения
- ✅ Обнаружение отключения устройства
- ✅ Поддержка только одного устройства одновременно
- ✅ Обработка случая, когда MIDI не поддерживается на устройстве

**Реализованные компоненты:**
- `TrackMidiConnectionUseCase` — Use Case для отслеживания подключений
- `MidiRepository` — интерфейс репозитория
- `MidiRepositoryImpl` — реализация репозитория
- `MidiDataSource` — Data Source для работы с Android MIDI API
- `MidiConnectionViewModel` — ViewModel для управления состоянием

## 3. Архитектурные решения

### 3.1. Соблюдение принципов Clean Architecture

Реализация полностью соответствует архитектурным принципам проекта:

- **Domain Layer (Ядро)** — независим от Android, содержит бизнес-логику
- **Data Layer** — реализует источники данных через Android MIDI API
- **Presentation Layer** — Android-специфичный слой для UI

### 3.2. Использованные паттерны

1. **MVVM (Model-View-ViewModel)**
   - `MidiConnectionViewModel` управляет UI-состоянием
   - Использует Use Cases из Domain-слоя

2. **Repository Pattern**
   - Интерфейс `MidiRepository` определен в Domain-слое
   - Реализация `MidiRepositoryImpl` в Data-слое
   - Изоляция Domain-слоя от деталей Android MIDI API

3. **Use Cases (Interactors)**
   - `TrackMidiConnectionUseCase` — координация отслеживания подключений

4. **Dependency Injection (Hilt)**
   - Все зависимости инжектируются через Hilt
   - Модули DI разделены по слоям

### 3.3. Реактивное программирование

- Использование Kotlin Flow для реактивного отслеживания изменений состояния
- `StateFlow` для хранения текущего состояния подключения
- Автоматическое обновление UI при изменении состояния

## 4. UML Диаграммы

### 4.1. Диаграмма слоев архитектуры

```plantuml
@startuml
!theme plain
skinparam packageStyle rectangle

package "Presentation Layer\n(Android-специфичный)" {
    class MainActivity
    class MidiConnectionViewModel {
        - trackMidiConnectionUseCase: TrackMidiConnectionUseCase
        - showConnectionNotificationUseCase: ShowConnectionNotificationUseCase
        - context: Context
        + connectionState: StateFlow<ConnectionState>
        + handleStateChange(state)
        - showToast(message)
    }
    class PianoFlowApplication
}

package "Domain Layer\n(Ядро - независимо от Android)" {
    class TrackMidiConnectionUseCase {
        - midiRepository: MidiRepository
        + invoke(): Flow<ConnectionState>
        + initialize()
    }
    interface MidiRepository {
        + getAvailableDevices(): List<MidiDevice>
        + connectToDevice(deviceId?): Result<MidiDevice>
        + disconnect()
        + observeConnectionState(): Flow<ConnectionState>
        + getCurrentConnectionState(): ConnectionState
    }
    class ConnectionState {
        <<sealed>>
        + Disconnected
        + Connecting
        + Connected(device)
        + Error(exception)
    }
    class MidiDevice {
        + id: Int
        + name: String
        + manufacturer: String?
        + isInput: Boolean
        + isOutput: Boolean
    }
    class MidiException {
        <<sealed>>
        + DeviceUnavailableException
        + PermissionDeniedException
        + ConnectionException
        + DeviceBusyException
        + MidiNotSupportedException
        + UnknownException
    }
}

package "Data Layer\n(Реализация источников данных)" {
    class MidiRepositoryImpl {
        - midiDataSource: MidiDataSource
        + getAvailableDevices(): List<MidiDevice>
        + connectToDevice(deviceId?): Result<MidiDevice>
        + disconnect()
        + observeConnectionState(): Flow<ConnectionState>
        + getCurrentConnectionState(): ConnectionState
    }
    class MidiDataSource {
        - midiManager: MidiManager?
        - _connectionState: MutableStateFlow<ConnectionState>
        + connectionState: StateFlow<ConnectionState>
        + getAvailableDevices(): List<MidiDeviceInfo>
        + connectToDevice(deviceId, callback)
        + disconnect()
        + getCurrentConnectionState(): ConnectionState
    }
    class DataModule {
        <<DI Module>>
        + provideMidiManager()
        + provideMidiDataSource()
        + provideMidiRepository()
    }
}

MainActivity --> MidiConnectionViewModel
MidiConnectionViewModel --> TrackMidiConnectionUseCase
<<include>> ./MIDI_NOTIFICATION.md
TrackMidiConnectionUseCase --> MidiRepository
MidiRepositoryImpl ..|> MidiRepository
MidiRepositoryImpl --> MidiDataSource
ConnectionState --> MidiDevice
ConnectionState --> MidiException
MidiDataSource --> ConnectionState

note right of MidiRepository
  Domain Layer не зависит
  от Android и может быть
  переиспользован
end note

note right of MidiRepositoryImpl
  Data Layer реализует
  интерфейсы из Domain Layer
  и работает с Android MIDI API
end note

@enduml
```

## 5. Структура компонентов

### 5.1. Domain Layer (Ядро)

#### Модели
- `MidiDevice` — модель MIDI-устройства
- `ConnectionState` — sealed class состояний подключения

#### Исключения
- `MidiException` — базовое исключение
  - `DeviceUnavailableException` — устройство недоступно
  - `PermissionDeniedException` — нет разрешения
  - `ConnectionException` — ошибка подключения
  - `DeviceBusyException` — устройство занято
  - `MidiNotSupportedException` — MIDI не поддерживается
  - `UnknownException` — неизвестная ошибка

#### Repository Interfaces
- `MidiRepository` — интерфейс для работы с MIDI-устройствами

#### Use Cases
- `TrackMidiConnectionUseCase` — отслеживание подключений (UC-001)

### 5.2. Data Layer

#### Data Sources
- `MidiDataSource` — работа с Android MIDI API
  - Автоматическое отслеживание устройств через `MidiManager.DeviceCallback`
  - Управление состоянием подключения
  - Обработка ошибок и преобразование в Domain-исключения

#### Repository Implementations
- `MidiRepositoryImpl` — реализация `MidiRepository`
  - Преобразование Android-моделей в Domain-модели
  - Координация работы с `MidiDataSource`

#### DI Modules
- `DataModule` — модуль DI для Data-слоя

### 5.3. Presentation Layer

#### ViewModels
- `MidiConnectionViewModel` — управление состоянием подключения

#### Activities
- `MainActivity` — главная Activity приложения

#### Application
- `PianoFlowApplication` — Application класс для инициализации Hilt

## 6. Зависимости

### 6.1. Добавленные зависимости

```kotlin
// Hilt для Dependency Injection
implementation("com.google.dagger:hilt-android:2.51.1")
ksp("com.google.dagger:hilt-compiler:2.51.1")

// Lifecycle для ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

// Coroutines для асинхронности
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// Activity KTX для viewModels()
implementation("androidx.activity:activity-ktx:1.8.0")

// KSP вместо Kapt (для поддержки Kotlin 2.0+)
ksp("com.google.devtools.ksp:2.0.21-1.0.27")
```

## 7. Тестирование

### 7.1. Unit-тесты

Реализованы unit-тесты для `TrackMidiConnectionUseCase`:
- Тест возврата Flow состояний
- Тест инициализации при отключенном состоянии
- Тест инициализации при уже подключенном устройстве
- Тест инициализации при отсутствии устройств

## 8. Соответствие критериям приемки

- ✅ При подключении MIDI-клавиатуры система автоматически подключается к первому найденному устройству.
- ✅ При отключении устройства система обновляет свое состояние.
- ✅ При ошибке подключения система переходит в состояние ошибки.
- ✅ Система реагирует на подключение/отключение устройства в реальном времени.
- ✅ При наличии нескольких устройств система подключается только к первому.

## 9. Заключение

Реализован полный функционал отслеживания подключения USB MIDI-клавиатуры согласно Use Case UC-001. Реализация соответствует архитектурным принципам проекта, использует Clean Architecture, покрыта unit-тестами и готова к использованию.

---

**Связанные документы:**
- [Use Cases: USB MIDI клавиатура](../uc/USB_MIDI_KEYBOARD.md)
- [Архитектурные принципы](../plans/ARCHITECTURE_PRINCIPLES.md)
- [Описание приложения](../plans/APPLICATION_DESCRIPTION.md)
