# Стратегия тестирования подключения MIDI-клавиатуры

## Цель документа

Данный документ описывает стратегию и инструменты для тестирования функционала подключения USB MIDI-клавиатуры в Android Studio с использованием виртуальных устройств (эмуляторов). Документ учитывает ограничения эмуляторов Android и предлагает комплексный подход к тестированию.

## Проблематика тестирования на виртуальных устройствах

### Ограничения эмуляторов Android

1. **Отсутствие поддержки реальных USB устройств**: Эмуляторы Android не поддерживают прямое подключение реальных USB MIDI-устройств
2. **Ограниченная поддержка MIDI API**: Некоторые функции MIDI API могут работать некорректно на эмуляторах
3. **Сложность эмуляции событий подключения/отключения**: Сложно симулировать реальные события подключения устройств

### Решения

1. **Моки и стабы** для изоляции тестируемого кода
2. **Виртуальные MIDI устройства** через ADB и специальные инструменты
3. **Многоуровневое тестирование** (Unit → Integration → UI)
4. **Комбинация эмуляторов и реальных устройств** для финальной проверки

---

## Стратегия тестирования

### 1. Unit-тесты (JUnit)

**Назначение**: Тестирование бизнес-логики без зависимости от Android API и реальных устройств.

**Что тестируем**:
- Use Cases (обработка подключения, обработка ошибок)
- Преобразование ошибок в пользовательские сообщения
- Логика выбора первого устройства из списка
- Валидация данных

**Инструменты**:
- `JUnit 5` или `JUnit 4`
- `MockK` или `Mockito` для создания моков
- `kotlinx.coroutines.test` для тестирования корутин
- `Turbine` для тестирования Flow

**Пример структуры**:
```
app/src/test/java/com/astrizhachuk/pianoflow/
├── domain/
│   ├── usecase/
│   │   ├── midi/
│   │   │   ├── ConnectMidiDeviceUseCaseTest.kt
│   │   │   └── ProcessMidiErrorUseCaseTest.kt
│   └── model/
│       └── MidiErrorMapperTest.kt
└── data/
    └── repository/
        └── MidiRepositoryImplTest.kt (с моками)
```

**Преимущества**:
- ✅ Быстрое выполнение
- ✅ Не требует эмулятора
- ✅ Легко покрыть все сценарии
- ✅ Изолированное тестирование логики

---

### 2. Интеграционные тесты с моками Android API

**Назначение**: Тестирование взаимодействия с Android MIDI API через моки.

**Что тестируем**:
- Работа с `MidiManager`
- Обработка событий подключения/отключения устройств
- Работа с `MidiDevice` и `MidiReceiver`
- Интеграция между слоями (Data → Domain → Presentation)

**Инструменты**:
- `Robolectric` — эмуляция Android-окружения на JVM
- `MockK` для моков Android-классов
- `androidx.test.core` для тестирования Android-компонентов

**Пример структуры**:
```
app/src/test/java/com/astrizhachuk/pianoflow/
└── data/
    └── datasource/
        └── midi/
            ├── MidiDataSourceTest.kt (с Robolectric)
            └── MidiReceiverTest.kt
```

**Преимущества**:
- ✅ Тестирование на JVM (быстро)
- ✅ Полный контроль над поведением Android API
- ✅ Возможность симулировать любые сценарии

---

### 3. Инструментальные тесты (AndroidTest) с виртуальными MIDI устройствами

**Назначение**: Тестирование на реальном Android-окружении (эмулятор) с эмуляцией MIDI-устройств.

#### 3.1. Использование ADB для эмуляции MIDI

**Инструмент**: `amidi` (Android MIDI) через ADB

**Подход**:
1. Использование виртуального MIDI порта через ADB
2. Отправка MIDI-сообщений через командную строку
3. Эмуляция подключения/отключения устройств

**Ограничения**: Требует настройки и может быть сложно автоматизировать.

#### 3.2. Использование библиотеки для виртуальных MIDI устройств

**Рекомендуемый подход**: Создание тестового модуля с виртуальным MIDI-устройством.

**Инструменты**:
- `android.media.midi.MidiManager` — для создания виртуальных устройств
- `android.media.midi.MidiDeviceInfo` — для эмуляции устройств
- `androidx.test.runner.AndroidJUnitRunner`

**Пример структуры**:
```
app/src/androidTest/java/com/astrizhachuk/pianoflow/
├── midi/
│   ├── VirtualMidiDeviceHelper.kt
│   ├── MidiConnectionTest.kt
│   └── MidiErrorHandlingTest.kt
└── ui/
    └── MidiConnectionUITest.kt
```

**Преимущества**:
- ✅ Тестирование на реальном Android-окружении
- ✅ Проверка работы с реальным MIDI API
- ✅ Возможность автоматизации

---

### 4. UI-тесты (Espresso)

**Назначение**: Тестирование пользовательского интерфейса и Toast-уведомлений.

**Что тестируем**:
- Отображение Toast-уведомлений при подключении
- Отображение Toast-уведомлений при ошибках
- Отсутствие уведомлений при отключении
- Состояние UI при различных сценариях

**Инструменты**:
- `Espresso` — для UI-тестирования
- `Espresso.idling` — для ожидания асинхронных операций
- `androidx.test.espresso` — базовые инструменты

**Пример структуры**:
```
app/src/androidTest/java/com/astrizhachuk/pianoflow/
└── ui/
    ├── MidiConnectionUITest.kt
    └── ToastMatcher.kt (кастомный матчер для Toast)
```

**Преимущества**:
- ✅ Проверка реального пользовательского опыта
- ✅ Валидация всех Use Cases из документации

---

## Инструменты и зависимости

### Gradle зависимости для тестирования

```kotlin
dependencies {
    // Unit-тесты
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0") // для Flow
    testImplementation("io.mockk:mockk:1.13.8") // для моков
    
    // Интеграционные тесты с Android API
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    
    // Инструментальные тесты
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    
    // Для работы с корутинами в тестах
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
```

---

## Покрытие Use Cases из документации

### UC-001: Отслеживание подключения MIDI-клавиатуры

| Сценарий | Тип теста | Инструмент |
|----------|-----------|------------|
| Основной поток (успешное подключение) | Unit + Integration + UI | JUnit + Robolectric + Espresso |
| A1: Устройство уже подключено | Unit + Integration | JUnit + Robolectric |
| A2: Ошибка при подключении | Unit + Integration + UI | JUnit + Robolectric + Espresso |
| A3: Отключение устройства | Unit + Integration + UI | JUnit + Robolectric + Espresso |
| A4: Несколько устройств | Unit + Integration | JUnit + Robolectric |
| E1: Нет разрешения | Unit + Integration + UI | JUnit + Robolectric + Espresso |
| E2: MIDI API недоступен | Unit + Integration + UI | JUnit + Robolectric + Espresso |

### UC-002: Обработка ошибок подключения

| Сценарий | Тип теста | Инструмент |
|----------|-----------|------------|
| Основной поток | Unit | JUnit + MockK |
| A1-A5: Различные типы ошибок | Unit | JUnit + MockK |
| E1: Ошибка при обработке ошибки | Unit | JUnit + MockK |

---

## Практические примеры

### Пример 1: Unit-тест для Use Case обработки ошибок

```kotlin
// app/src/test/java/com/astrizhachuk/pianoflow/domain/usecase/midi/ProcessMidiErrorUseCaseTest.kt

import com.astrizhachuk.pianoflow.domain.usecase.midi.ProcessMidiErrorUseCase
import com.astrizhachuk.pianoflow.domain.exception.MidiException
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ProcessMidiErrorUseCaseTest {
    
    @Test
    fun `process error - device unavailable returns correct message`() = runTest {
        // Given
        val useCase = ProcessMidiErrorUseCase()
        val error = MidiException.DeviceUnavailable()
        
        // When
        val result = useCase(error)
        
        // Then
        assertEquals(
            "Устройство недоступно. Проверьте подключение.",
            result.message
        )
    }
    
    @Test
    fun `process error - permission denied returns correct message`() = runTest {
        // Given
        val useCase = ProcessMidiErrorUseCase()
        val error = MidiException.PermissionDenied()
        
        // When
        val result = useCase(error)
        
        // Then
        assertEquals(
            "Нет разрешения на доступ к MIDI-устройству.",
            result.message
        )
    }
    
    @Test
    fun `process error - unknown error returns generic message`() = runTest {
        // Given
        val useCase = ProcessMidiErrorUseCase()
        val error = Exception("Unknown error")
        
        // When
        val result = useCase(error)
        
        // Then
        assertEquals(
            "Произошла ошибка при подключении к устройству.",
            result.message
        )
    }
}
```

### Пример 2: Интеграционный тест с Robolectric

```kotlin
// app/src/test/java/com/astrizhachuk/pianoflow/data/datasource/midi/MidiDataSourceTest.kt

import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import androidx.test.core.app.ApplicationProvider
import com.astrizhachuk.pianoflow.data.datasource.midi.MidiDataSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23])
class MidiDataSourceTest {
    
    private lateinit var midiDataSource: MidiDataSource
    private lateinit var midiManager: MidiManager
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext()
        midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
        midiDataSource = MidiDataSource(midiManager)
    }
    
    @Test
    fun `scan devices returns list of available devices`() = runTest {
        // Given - мокируем устройства
        val mockDevice = mockk<MidiDeviceInfo>()
        // ... настройка моков
        
        // When
        val devices = midiDataSource.scanDevices()
        
        // Then
        assertNotNull(devices)
    }
}
```

### Пример 3: UI-тест для проверки Toast-уведомлений

```kotlin
// app/src/androidTest/java/com/astrizhachuk/pianoflow/ui/MidiConnectionUITest.kt

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.astrizhachuk.pianoflow.MainActivity
import com.astrizhachuk.pianoflow.ui.ToastMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MidiConnectionUITest {
    
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Test
    fun testConnectionSuccessToast() {
        // Given - эмулируем подключение устройства
        // (через виртуальное MIDI устройство или мок)
        
        // When - устройство подключается
        
        // Then - проверяем Toast
        onView(withText("MIDI-клавиатура подключена"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testConnectionErrorToast() {
        // Given - эмулируем ошибку подключения
        
        // When - происходит ошибка
        
        // Then - проверяем Toast с сообщением об ошибке
        onView(withText("Устройство недоступно. Проверьте подключение."))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testDisconnectionNoToast() {
        // Given - устройство подключено
        
        // When - устройство отключается
        
        // Then - Toast не отображается
        // (проверка отсутствия Toast)
    }
}
```

### Пример 3.1: Кастомный матчер для Toast

```kotlin
// app/src/androidTest/java/com/astrizhachuk/pianoflow/ui/ToastMatcher.kt

import android.view.View
import android.widget.Toast
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import androidx.test.espresso.Root

class ToastMatcher : TypeSafeMatcher<Root>() {
    
    override fun describeTo(description: Description) {
        description.appendText("is toast")
    }
    
    override fun matchesSafely(root: Root): Boolean {
        val type = root.windowLayoutParams.get().type
        if (type == android.view.WindowManager.LayoutParams.TYPE_TOAST) {
            val windowToken = root.decorView.windowToken
            val appToken = root.decorView.applicationWindowToken
            if (windowToken === appToken) {
                // windowToken == appToken означает, что это не подмененное окно
                return true
            }
        }
        return false
    }
}
```

**Использование**:
```kotlin
// В тестах
onView(withText("MIDI-клавиатура подключена"))
    .inRoot(ToastMatcher())
    .check(matches(isDisplayed()))
```

### Пример 4: Вспомогательный класс для виртуального MIDI устройства

```kotlin
// app/src/androidTest/java/com/astrizhachuk/pianoflow/midi/VirtualMidiDeviceHelper.kt

import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper

class VirtualMidiDeviceHelper(
    private val midiManager: MidiManager
) {
    
    /**
     * Создает виртуальное MIDI устройство для тестирования
     */
    fun createVirtualDevice(): MidiDeviceInfo {
        // Создание виртуального устройства через MidiManager
        // (требует Android API 26+)
        return midiManager.createDevice(
            /* inputPortCount = */ 0,
            /* outputPortCount = */ 1
        ).deviceInfo
    }
    
    /**
     * Эмулирует подключение устройства
     */
    fun simulateDeviceConnection(deviceInfo: MidiDeviceInfo) {
        // Симуляция события подключения
        // Можно использовать BroadcastReceiver или callback
    }
    
    /**
     * Эмулирует отправку MIDI-сообщения
     */
    fun sendMidiMessage(device: MidiDevice, message: ByteArray) {
        val receiver = device.openInputPort(0)
        Handler(Looper.getMainLooper()).post {
            receiver.send(message, 0, message.size, System.nanoTime())
        }
    }
}
```

---

## Рекомендации по организации тестов

### Структура тестовых классов

1. **Именование**: `[ClassName]Test.kt` для unit-тестов, `[ClassName]Test.kt` для Android-тестов
2. **Организация**: Повторять структуру основного кода
3. **Покрытие**: Минимум 80% для бизнес-логики (Use Cases, обработка ошибок)

### Приоритеты тестирования

1. **Высокий приоритет**:
   - Use Cases (UC-001, UC-002)
   - Обработка ошибок
   - Преобразование ошибок в сообщения

2. **Средний приоритет**:
   - Интеграция между слоями
   - Работа с MidiManager
   - UI-тесты для Toast

3. **Низкий приоритет**:
   - Граничные случаи
   - Производительность
   - Стресс-тесты

---

## Альтернативные подходы для эмуляции MIDI

### 1. Использование реального устройства для финальной проверки

**Когда использовать**: После успешного прохождения всех автоматических тестов.

**Процесс**:
1. Подключить реальную MIDI-клавиатуру к физическому Android-устройству
2. Запустить приложение
3. Проверить все Use Cases вручную
4. Задокументировать результаты

### 2. Использование MIDI-симуляторов на ПК

**Инструменты**:
- **LoopMIDI** (Windows) — создание виртуальных MIDI портов
- **MIDI-OX** (Windows) — мониторинг и отправка MIDI-сообщений
- **Hairless MIDI** (кроссплатформенный) — мониторинг MIDI

**Подход**:
1. Создать виртуальный MIDI порт на ПК
2. Подключить Android-устройство через USB (режим разработчика)
3. Использовать ADB для пересылки MIDI-сообщений (если возможно)

**Ограничения**: Сложно автоматизировать, требует ручной настройки.

### 3. Использование библиотек для тестирования

**Рекомендуемые библиотеки**:
- `androidx.test` — базовые инструменты
- `MockK` — для моков Kotlin-кода
- `Turbine` — для тестирования Flow

---

## Чек-лист для реализации тестирования

### Этап 1: Настройка инфраструктуры
- [ ] Добавить зависимости для тестирования в `build.gradle.kts`
- [ ] Настроить структуру тестовых директорий
- [ ] Создать базовые тестовые классы

### Этап 2: Unit-тесты
- [ ] Тесты для Use Cases (UC-001, UC-002)
- [ ] Тесты для обработки ошибок
- [ ] Тесты для маппинга ошибок в сообщения
- [ ] Покрытие минимум 80%

### Этап 3: Интеграционные тесты
- [ ] Тесты с Robolectric для работы с MidiManager
- [ ] Тесты для MidiDataSource
- [ ] Тесты для MidiRepository

### Этап 4: UI-тесты
- [ ] Тесты для Toast-уведомлений
- [ ] Тесты для различных сценариев подключения
- [ ] Кастомный матчер для Toast

### Этап 5: Инструментальные тесты
- [ ] Создать VirtualMidiDeviceHelper
- [ ] Тесты на эмуляторе с виртуальными устройствами
- [ ] Автоматизация сценариев из Use Cases

### Этап 6: Документация
- [ ] Задокументировать процесс запуска тестов
- [ ] Создать инструкцию по добавлению новых тестов
- [ ] Обновить CI/CD для автоматического запуска тестов

---

## Заключение

Предложенная стратегия обеспечивает:

1. **Полное покрытие функционала** через многоуровневое тестирование
2. **Возможность тестирования на эмуляторах** через моки и виртуальные устройства
3. **Быструю обратную связь** через unit-тесты
4. **Проверку реального поведения** через инструментальные тесты
5. **Валидацию пользовательского опыта** через UI-тесты

**Рекомендация**: Начать с unit-тестов и постепенно добавлять интеграционные и UI-тесты. Для финальной проверки использовать реальное устройство с физической MIDI-клавиатурой.

---

## Связанные документы

- [Use Cases для USB MIDI-клавиатуры](../uc/USB_MIDI_KEYBOARD.md) — описание функциональных требований
- [Архитектурные принципы](ARCHITECTURE_PRINCIPLES.md) — структура приложения
- [План MIDI-тестирования](MIDI_TEST_PLAN.md) — план реализации подключения

