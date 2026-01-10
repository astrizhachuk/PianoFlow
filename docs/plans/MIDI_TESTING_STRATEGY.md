# Стратегия тестирования MIDI-функционала в Android

## Введение

Этот документ описывает стратегию тестирования функционала, связанного с подключением и взаимодействием с MIDI-устройствами в Android-приложении. Основной акцент сделан на автоматизированных тестах и методах эмуляции MIDI-устройств, что позволяет проводить эффективное тестирование без необходимости использования физического оборудования на каждом этапе разработки.

За основу взяты рекомендации из [официальной документации Android по тестированию](https://developer.android.com/training/testing) и [тестированию с Hilt](https://developer.android.com/training/dependency-injection/hilt-testing).

## Проблема: Тестирование без реальных устройств

Прямое тестирование с физическими MIDI-устройствами сложно автоматизировать и интегрировать в CI/CD пайплайны. Эмуляторы Android не предоставляют встроенной поддержки для проброса USB-устройств, что делает невозможным прямое подключение MIDI-клавиатуры к виртуальному устройству.

**Решение** — многоуровневая стратегия тестирования с использованием Hilt для внедрения зависимостей, которая включает в себя:
1.  **Unit-тесты** для проверки изолированной бизнес-логики.
2.  **Интеграционные тесты** для проверки взаимодействия компонентов.
3.  **Инструментальные (UI) тесты** с использованием Hilt для подмены зависимостей и эмуляции MIDI-устройств.

---

## 1. Unit-тесты (Локальные тесты)

**Цель**: Проверить отдельные компоненты приложения (классы, функции) в изоляции от Android Framework и внешних зависимостей. Для классов, использующих constructor injection, Hilt не требуется — зависимости можно передать вручную.

**Что тестируем**:
*   **ViewModel / Presenter**: Корректность обновления UI State.
*   **Use Cases / Interactors**: Бизнес-логику.
*   **Mappers**: Логику преобразования данных.

**Инструменты**:
*   `JUnit 5` / `JUnit 4`, `MockK` / `Mockito`, `kotlinx-coroutines-test`, `Turbine`.

**Пример (тестирование ViewModel)**:
```kotlin
// Расположение: app/src/test/java/com/astrizhachuk/pianoflow/ui/MyViewModelTest.kt

class MyViewModelTest {

    @Test
    fun `midi device connection success updates state`() = runTest {
        // Given
        val midiRepository = mockk<MidiRepository>()
        coEvery { midiRepository.connectToFirstDevice() } returns Result.success(Unit)
        
        val viewModel = MyViewModel(midiRepository) // Зависимость передается вручную
        
        // When
        viewModel.connectToDevice()
        
        // Then
        assertTrue(viewModel.uiState.value.isConnected)
    }
}
```

---

## 2. Интеграционные тесты (Локальные тесты)

**Цель**: Проверить взаимодействие нескольких компонентов с использованием фреймворков, эмулирующих среду Android.

**Что тестируем**:
*   **Repository и DataSource**: Взаимодействие репозитория с `MidiDataSource`.
*   **Взаимодействие с моками Android API**: Проверка вызовов `MidiManager`.

**Инструменты**:
*   `Robolectric`, `MockK` / `Mockito`.

**Пример (тестирование `MidiDataSource` с Robolectric)**:
```kotlin
// Расположение: app/src/test/java/com/astrizhachuk/pianoflow/data/datasource/midi/MidiDataSourceTest.kt

@RunWith(RobolectricTestRunner::class)
class MidiDataSourceTest {

    private lateinit var midiManager: MidiManager
    private lateinit var dataSource: MidiDataSource

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        midiManager = mockk(relaxed = true)
        shadowOf(context).setSystemService(Context.MIDI_SERVICE, midiManager)
        
        dataSource = MidiDataSource(midiManager)
    }

    @Test
    fun `scanDevices returns available devices`() = runTest {
        // Given
        val mockDeviceInfo = mockk<MidiDeviceInfo>()
        every { midiManager.devices } returns arrayOf(mockDeviceInfo)
        
        // When
        val devices = dataSource.scanDevices()
        
        // Then
        assertEquals(1, devices.size)
    }
}
```

---

## 3. Инструментальные тесты (AndroidTest) с Hilt

**Цель**: Протестировать приложение в реальной среде Android (на эмуляторе) с подменой реальных зависимостей на тестовые.

### Настройка Hilt для тестов

1.  **Добавьте зависимости**: `hilt-android-testing` и `kaptAndroidTest` для `hilt-compiler`.
2.  **Создайте Test Runner**: для запуска тестов с `HiltTestApplication`.
3.  **Аннотируйте тесты**: Используйте `@HiltAndroidTest` для тестовых классов и `HiltAndroidRule`.

### Подмена зависимостей и эмуляция MIDI

С помощью Hilt мы можем легко заменить настоящий `MidiDataSource`, который работает с системным `MidiManager`, на его фейковую версию. Этот фейк будет взаимодействовать с нашим `VirtualMidiDeviceHelper` для полной эмуляции.

**Пример UI-теста с Hilt и `@BindValue`**:

```kotlin
// Расположение: app/src/androidTest/java/com/astrizhachuk/pianoflow/ui/MidiConnectionUITest.kt

// 1. Указываем Hilt, что нужно деинсталлировать настоящий модуль
@UninstallModules(MidiModule::class) 
@HiltAndroidTest
class MidiConnectionUITest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    // 2. Создаем фейковый источник данных
    private val fakeMidiDataSource = FakeMidiDataSource()

    // 3. С помощью @BindValue «подкладываем» фейк в граф зависимостей
    @BindValue
    val midiDataSource: MidiDataSource = fakeMidiDataSource

    private val virtualMidiDevice = VirtualMidiDeviceHelper()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun connectionSuccess_showsSuccessMessage() {
        // When
        // Симулируем подключение виртуального устройства
        virtualMidiDevice.createAndRegisterDevice()
        // Имитируем, что наш фейковый источник данных обнаружил это устройство
        fakeMidiDataSource.simulateDeviceConnection(virtualMidiDevice.getDeviceInfo())

        // Then
        // Проверяем, что на экране появился текст о успешном подключении
        onView(withText("MIDI-клавиатура подключена"))
            .check(matches(isDisplayed()))
    }
}
```

### Вспомогательные классы для теста

```kotlin
// Расположение: app/src/androidTest/java/com/astrizhachuk/pianoflow/midi/FakeMidiDataSource.kt

// Фейковый источник данных, который мы будем внедрять в тестах
class FakeMidiDataSource : MidiDataSource {
    private val _connectedDevices = MutableStateFlow<List<MidiDeviceInfo>>(emptyList())
    override val connectedDevices: StateFlow<List<MidiDeviceInfo>> = _connectedDevices

    fun simulateDeviceConnection(deviceInfo: MidiDeviceInfo) {
        _connectedDevices.value = listOf(deviceInfo)
    }
    
    // Остальные методы интерфейса можно оставить пустыми или мокировать
}

// Расположение: app/src/androidTest/java/com/astrizhachuk/pianoflow/midi/VirtualMidiDeviceHelper.kt

class VirtualMidiDeviceHelper {
    // ... (код хелпера без изменений) ...
    
    fun getDeviceInfo(): MidiDeviceInfo {
        // Возвращаем информацию о созданном виртуальном устройстве
        // ...
    }
}
```

---

## 4. Ручное тестирование и отладка

После того как автоматизированные тесты подтвердили корректность основной логики, необходимо провести ручную проверку на реальных и виртуальных устройствах. Этот этап является финальной проверкой перед релизом.

### Подключение к физическому устройству

Это наиболее надежный способ проверить полную работоспособность MIDI-функционала, включая корректную работу USB-хоста на Android-устройстве.

**Необходимое оборудование:**
*   Физическое Android-устройство (смартфон, планшет).
*   MIDI-клавиатура или цифровое пианино с USB-MIDI или MIDI-выходом.
*   **USB OTG-адаптер** (On-The-Go): для подключения USB-устройства к Android-смартфону/планшету.

**Процесс проверки:**
1.  С помощью OTG-адаптера подключите MIDI-клавиатуру к Android-устройству.
2.  Дождитесь системного уведомления о подключении MIDI-устройства.
3.  Запустите ваше приложение и убедитесь, что оно обнаружило новое устройство и отображает его в списке доступных.
4.  Проверьте взаимодействие: нажатие клавиш на MIDI-клавиатуре должно вызывать соответствующие события в приложении.

**Инструменты для отладки на физическом устройстве:**
*   **Logcat в Android Studio**: основной инструмент. Фильтруйте логи по тегам, связанным с `MidiManager` или вашими собственными классами, чтобы отслеживать процесс обнаружения устройства и обработки MIDI-сообщений.
*   **Сторонние MIDI-утилиты** (например, "MIDI Scope" или "MIDI Device Info" из Google Play): установите их на устройство, чтобы независимо проверить, видит ли операционная система подключенную клавиатуру и получает ли от нее данные. Это помогает быстро определить, где проблема: на уровне системы или в коде вашего приложения.

### Подключение к эмулятору Android

Стандартный эмулятор Android Studio **не поддерживает** прямое подключение (проброс) USB-устройств с хост-компьютера. Поэтому протестировать физическое подключение MIDI-клавиатуры к эмулятору невозможно.

Однако можно **симулировать** получение MIDI-сообщений, отправив их на эмулятор по сети.

**Обходной путь: MIDI через сеть (Network MIDI)**
Этот метод позволяет протестировать логику обработки MIDI-данных в приложении, но не сам процесс USB-подключения.

**Процесс симуляции:**
1.  **На компьютер (Windows/macOS)** устанавливается программа, которая создает виртуальный MIDI-порт и транслирует MIDI-данные в сеть (например, `rtpMIDI` для macOS или `ipMIDI` для Windows).
2.  **На эмулятор Android** устанавливается приложение-приемник (например, "MIDI Network Receiver" из Google Play), которое "слушает" сеть и создает в системе Android виртуальное MIDI-устройство.
3.  В настройках вашего приложения (или в системных настройках для разработчиков) в качестве источника MIDI-сигналов выбирается это виртуальное сетевое устройство.

Таким образом, вы можете играть на физической клавиатуре, подключенной к компьютеру, или использовать виртуальную клавиатуру на ПК, а MIDI-сообщения будут поступать в ваше приложение на эмуляторе.

---

## Зависимости для тестирования

```kotlin
dependencies {
    // ... основные зависимости ...

    // Hilt for Android
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")

    // Hilt For instrumentation tests
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    kaptAndroidTest("com.google.dagger:hilt-compiler:2.51.1")

    // Hilt For local unit tests
    testImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    kaptTest("com.google.dagger:hilt-compiler:2.51.1")
}
```

## Заключение

Использование Hilt для подмены зависимостей в инструментальных тестах — ключ к надежному и полностью автоматизированному тестированию MIDI-функционала. Эта стратегия позволяет изолировать UI от реальной системной логики, симулируя любые сценарии подключения и взаимодействия с устройствами. Ручное тестирование на реальных устройствах остается необходимым, но его роль сводится к финальной проверке.
