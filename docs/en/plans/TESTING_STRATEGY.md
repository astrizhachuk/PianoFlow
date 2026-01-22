# MIDI functionality testing strategy in Android

## Introduction

This document describes a strategy for testing functionality related to connecting and interacting with MIDI devices in an Android application. The main emphasis is on automated tests and methods for emulating MIDI devices, which allows for effective testing without the need for physical equipment at every stage of development.

The recommendations from the [official Android documentation on testing](https://developer.android.com/training/testing) and [testing with Hilt](https://developer.android.com/training/dependency-injection/hilt-testing) are taken as a basis.

## The problem: Testing without real devices

Direct testing with physical MIDI devices is difficult to automate and integrate into CI/CD pipelines. Android emulators do not provide built-in support for USB device passthrough, which makes it impossible to directly connect a MIDI keyboard to a virtual device.

**The solution** is a multi-level testing strategy using Hilt for dependency injection, which includes:
1.  **Unit tests** to check isolated business logic.
2.  **Integration tests** to check the interaction of components.
3.  **Instrumental (UI) tests** using Hilt to replace dependencies and emulate MIDI devices.

---

## 1. Unit tests (Local tests)

**Goal**: To check individual application components (classes, functions) in isolation from the Android Framework and external dependencies. For classes that use constructor injection, Hilt is not required — dependencies can be passed manually.

**What we are testing**:
*   **ViewModel / Presenter**: Correctness of UI State updates.
*   **Use Cases / Interactors**: Business logic.
*   **Mappers**: Data transformation logic.

**Tools**:
*   `JUnit 5` / `JUnit 4`, `MockK`, `kotlinx-coroutines-test`, `Turbine`.

**Example (ViewModel testing)**:
```kotlin
// Location: app/src/test/java/com/astrizhachuk/pianoflow/ui/MyViewModelTest.kt

class MyViewModelTest {

    @Test
    fun `midi device connection success updates state`() = runTest {
        // Given
        val midiRepository = mockk<MidiRepository>()
        coEvery { midiRepository.connectToFirstDevice() } returns Result.success(Unit)
        
        val viewModel = MyViewModel(midiRepository) // The dependency is passed manually
        
        // When
        viewModel.connectToDevice()
        
        // Then
        assertTrue(viewModel.uiState.value.isConnected)
    }
}
```

---

## 2. Integration tests (Local tests)

**Goal**: To check the interaction of several components using frameworks that emulate the Android environment.

**What we are testing**:
*   **Repository and DataSource**: Interaction of the repository with `MidiDataSource`.
*   **Interaction with mocks, or with a "shadow" implementation of Android API classes**: Checking `MidiManager` calls.

**Tools**:
*   `Robolectric`, `MockK` / `Mockito`.

### Useful links to the official documentation

- **Robolectric:**
    - [Official website](http://robolectric.org/)
    - [Guide to Shadow objects](http://robolectric.org/extending/)
- **Mockito and mockito-kotlin:**
    - [Official Mockito website](https://site.mockito.org/)
    - [Mockito-kotlin documentation (syntax for Kotlin)](https://github.com/mockito/mockito-kotlin)

**Example (testing `MidiDataSource` with Robolectric)**:

```kotlin
// Location: app/src/test/java/com/astrizhachuk/pianoflow/data/datasource/midi/MidiDataSourceTest.kt

@RunWith(RobolectricTestRunner::class)
class MidiDataSourceTest {

    private lateinit var context: Context
    private lateinit var midiManager: MidiManager
    private lateinit var shadowMidiManager: ShadowMidiManager

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
        shadowMidiManager = shadowOf(midiManager)
    }

    @Test
    fun `when a device is available on init then it is opened`() = runTest {
        // Given
        val mockDeviceInfo = createMockDeviceInfo("Test MIDI") // Helper function to create a mock
        shadowMidiManager.addDevice(mockDeviceInfo)
        
        // When
        val dataSource = MidiDataSource(context, mock()) // mock() for the mapper
        shadowOf(Looper.getMainLooper()).idle() // Give time for callbacks to execute
        
        // Then
        val openedDevice = shadowMidiManager.openedDevices.first()
        assertEquals(mockDeviceInfo, openedDevice.info)
    }
}
```

---

## 3. Instrumental tests (AndroidTest) with Hilt

**Goal**: To test the application in a real Android environment (on an emulator) with the replacement of real dependencies with test ones.

**What we are testing**:
*   **User Flows**: End-to-end checks, for example: "connected the device -> a notification appeared on the screen".
*   **UI correctness**: Checking that the UI (`Activity`/`Fragment`) correctly reacts to state changes coming from the `ViewModel`.
*   **Interaction with fake dependencies**: We make sure that the entire application works correctly when the real `MidiDataSource` is replaced with its fake counterpart.

**Tools**:
*   **Hilt** (`@HiltAndroidTest`, `@BindValue`, `HiltAndroidRule`): for managing the life cycle of components and replacing dependencies.
*   **Espresso**: for simulating user actions (clicks, swipes) and checking the state of UI elements (`onView`, `check`, `matches`).
*   **ActivityScenarioRule** / **FragmentScenario**: for controlled launch of screens.
*   **`Fake` implementations**: to simulate the behavior of external dependencies.

### Useful links to the official documentation

*   **[Testing with Hilt](https://developer.android.com/training/dependency-injection/hilt-testing)**: The main resource that describes all aspects, including `@HiltAndroidTest`, dependency replacement, and integration.
*   **[Testing the UI in Android (Espresso)](https://developer.android.com/training/testing/ui-testing)**: Documentation on Espresso, `ActivityScenarioRule`, and the basics of writing UI tests.
*   **[Replacing dependencies in tests](https://developer.android.com/training/dependency-injection/hilt-testing#replace-binding)**: A detailed section on replacing dependencies using `@UninstallModules` and test modules.

### Setting up Hilt for tests

1.  **Add dependencies**: `hilt-android-testing` and `kaptAndroidTest` for `hilt-compiler`.
2.  **Create a Test Runner**: to run tests with `HiltTestApplication`.
3.  **Annotate tests**: Use `@HiltAndroidTest` for test classes and `HiltAndroidRule`.

### Replacing dependencies and emulating MIDI

With Hilt, we can easily replace the real `MidiDataSource`, which works with the system `MidiManager`, with its fake version. This fake will interact with our `VirtualMidiDeviceHelper` for full emulation.

**Example of a UI test with Hilt and `@BindValue`**:

```kotlin
// Location: app/src/androidTest/java/com/astrizhachuk/pianoflow/ui/MidiConnectionUITest.kt

// 1. We tell Hilt that the real module needs to be uninstalled
@UninstallModules(MidiModule::class) 
@HiltAndroidTest
class MidiConnectionUITest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    // 2. Create a fake data source
    private val fakeMidiDataSource = FakeMidiDataSource()

    // 3. Using @BindValue, we "substitute" the fake into the dependency graph
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
        // Simulate the connection of a virtual device
        virtualMidiDevice.createAndRegisterDevice()
        // We imitate that our fake data source has detected this device
        fakeMidiDataSource.simulateDeviceConnection(virtualMidiDevice.getDeviceInfo())

        // Then
        // We check that the text about the successful connection appeared on the screen
        onView(withText("MIDI keyboard connected"))
            .check(matches(isDisplayed()))
    }
}
```

### Helper classes for the test

```kotlin
// Location: app/src/androidTest/java/com/astrizhachuk/pianoflow/midi/FakeMidiDataSource.kt

// A fake data source that we will inject in tests
class FakeMidiDataSource : MidiDataSource {
    private val _connectedDevices = MutableStateFlow<List<MidiDeviceInfo>>(emptyList())
    override val connectedDevices: StateFlow<List<MidiDeviceInfo>> = _connectedDevices

    fun simulateDeviceConnection(deviceInfo: MidiDeviceInfo) {
        _connectedDevices.value = listOf(deviceInfo)
    }
    
    // The remaining methods of the interface can be left empty or mocked
}

// Location: app/src/androidTest/java/com/astrizhachuk/pianoflow/midi/VirtualMidiDeviceHelper.kt

class VirtualMidiDeviceHelper {
    // ... (helper code without changes) ...
    
    fun getDeviceInfo(): MidiDeviceInfo {
        // We return information about the created virtual device
        // ...
    }
}
```

---

## 4. Manual testing and debugging

After automated tests have confirmed the correctness of the main logic, it is necessary to conduct a manual check on real and virtual devices. This stage is the final check before the release.

### Connecting to a physical device

This is the most reliable way to check the full functionality of the MIDI functionality, including the correct operation of the USB host on an Android device.

**Required equipment:**
*   A physical Android device (smartphone, tablet).
*   A MIDI keyboard or digital piano with a USB-MIDI or MIDI output.
*   **USB OTG adapter** (On-The-Go): for connecting a USB device to an Android smartphone/tablet.

**Verification process:**
1.  Using an OTG adapter, connect the MIDI keyboard to the Android device.
2.  Wait for the system notification about the connection of the MIDI device.
3.  Launch your application and make sure that it has detected the new device and displays it in the list of available ones.
4.  Check the interaction: pressing the keys on the MIDI keyboard should cause corresponding events in the application.

**Tools for debugging on a physical device:**
*   **Logcat in Android Studio**: the main tool. Filter logs by tags related to `MidiManager` or your own classes to track the process of device detection and MIDI message processing.
*   **Third-party MIDI utilities** (for example, "MIDI Scope" or "MIDI Device Info" from Google Play): install them on the device to independently check whether the operating system sees the connected keyboard and receives data from it. This helps to quickly determine where the problem is: at the system level or in the code of your application.

### Connecting to the Android emulator

The standard Android Studio emulator **does not support** direct connection (passthrough) of USB devices from the host computer. Therefore, it is impossible to test the physical connection of a MIDI keyboard to an emulator.

However, you can **simulate** receiving MIDI messages by sending them to the emulator over the network.

**Workaround: MIDI over network (Network MIDI)**
This method allows you to test the logic of processing MIDI data in the application, but not the USB connection process itself.

**Simulation process:**
1.  **On the computer (Windows/macOS)**, a program is installed that creates a virtual MIDI port and broadcasts MIDI data to the network (for example, `rtpMIDI` for macOS or `ipMIDI` for Windows).
2.  **On the Android emulator**, a receiver application is installed (for example, "MIDI Network Receiver" from Google Play), which "listens" to the network and creates a virtual MIDI device in the Android system.
3.  In the settings of your application (or in the system settings for developers), this virtual network device is selected as the source of MIDI signals.

Thus, you can play on a physical keyboard connected to a computer, or use a virtual keyboard on a PC, and MIDI messages will be sent to your application on the emulator.

---

## Dependencies for testing

Below are the main dependencies used for various types of testing in the project, as indicated in the `build.gradle.kts` file.

```kotlin
dependencies {
    // ...

    // --- Unit tests (local, src/test) ---

    // The main framework for writing and running tests
    testImplementation(libs.junit)

    // Creating mocks (mock objects) to isolate dependencies
    testImplementation(libs.mockk)

    // Testing coroutines and asynchronous code
    testImplementation(libs.coroutines.test)

    // A utility for testing Kotlin Flow
    testImplementation(libs.turbine)
    
    // Emulation of the Android environment for running tests on the JVM
    testImplementation(libs.robolectric)

    // --- Instrumental tests (on a device/emulator, src/androidTest) ---

    // JUnit extensions for Android tests
    androidTestImplementation(libs.androidx.junit)

    // A framework for UI testing
    androidTestImplementation(libs.androidx.espresso.core)

    // Tracing
    implementation(libs.androidx.tracing.ktx)

    // Hilt support in instrumental tests for dependency injection
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
```
