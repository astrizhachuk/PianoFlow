package com.astrizhachuk.pianoflow.data.datasource.midi

import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.os.Handler
import android.os.Looper
import com.astrizhachuk.pianoflow.domain.mapper.midi.MidiDeviceMapper
import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import android.media.midi.MidiDevice as MidiDeviceApi

/**
 * Источник данных, инкапсулирующий всю логику работы с Android MIDI API.
 *
 * Этот класс отвечает за:
 * - Регистрацию и отмену регистрации обратных вызовов для отслеживания подключения/отключения MIDI-устройств.
 * - Открытие и закрытие соединения с MIDI-устройствами.
 * - Предоставление состояния подключения в виде `Flow` для остальной части приложения.
 *
 * Является синглтоном (@Singleton), так как должен существовать в единственном экземпляре на протяжении
 * всего жизненного цикла приложения, чтобы непрерывно отслеживать состояние MIDI-устройств.
 *
 * @param context Контекст приложения, необходимый для доступа к системным сервисам, таким как [MidiManager].
 * @param midiDeviceMapper Маппер для преобразования системной модели [MidiDeviceInfo] в доменную модель.
 */
class MidiDataSource @Inject constructor(
    context: Context,
    private val midiDeviceMapper: MidiDeviceMapper
) {
    private val midiManager = context.getSystemService(MidiManager::class.java)
    private var openedDevice: MidiDeviceApi? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.NoDevice)
    /**
     * Публичный поток (Flow), представляющий текущее состояние подключения MIDI-устройства.
     *
     * Подписчики могут отслеживать изменения и реагировать на состояния:
     * - [ConnectionState.NoDevice]: Устройства не найдены.
     * - [ConnectionState.Connected]: Устройство успешно подключено.
     * - [ConnectionState.Disconnected]: Устройство было отключено.
     * - [ConnectionState.Error]: Произошла ошибка.
     */
    val connectionState = _connectionState.asStateFlow()

    /**
     * Обратный вызов для системного [MidiManager], который отслеживает физическое
     * подключение и отключение MIDI-устройств к Android-устройству.
     */
    private val deviceCallback = object : MidiManager.DeviceCallback() {
        /**
         * Вызывается системой при подключении нового MIDI-устройства.
         */
        override fun onDeviceAdded(device: MidiDeviceInfo) {
            Timber.i("onDeviceAdded: Device detected: %s", device.properties.getString(MidiDeviceInfo.PROPERTY_NAME))
            openFirstAvailableDevice()
        }

        /**
         * Вызывается системой при отключении MIDI-устройства.
         * Если отключенное устройство является текущим открытым устройством, закрывает его.
         */
        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            Timber.i("onDeviceRemoved: Device disconnected: %s", device.properties.getString(MidiDeviceInfo.PROPERTY_NAME))
            if (isCurrentDevice(device)) {
                Timber.d("onDeviceRemoved: Disconnected device is current, closing.")
                closeDevice()
            }
        }
    }

    init {
        Timber.i("init: Initializing.")
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            Timber.e("init: MIDI feature NOT supported.")
            _connectionState.value = ConnectionState.Error("MIDI API не поддерживается на этом устройстве.")
        } else {
            Timber.i("init: MIDI feature supported.")
            val handler = Handler(Looper.getMainLooper())
            midiManager?.registerDeviceCallback(deviceCallback, handler)
            openFirstAvailableDevice()
        }
    }

    /**
     * Ищет первое доступное MIDI-устройство в системе и инициирует его открытие.
     * Если устройства не найдены, устанавливает состояние в [ConnectionState.NoDevice].
     * Безопасно обрабатывает SecurityException, если у приложения нет разрешений.
     */
    private fun openFirstAvailableDevice() {
        Timber.d("openFirstAvailableDevice: Looking for devices.")
        try {
            val firstDevice = midiManager?.devices?.firstOrNull()
            if (firstDevice != null) {
                Timber.i("openFirstAvailableDevice: Found device: %s. Attempting to open.", firstDevice.properties.getString(MidiDeviceInfo.PROPERTY_NAME))
                openDevice(firstDevice)
            } else {
                Timber.i("openFirstAvailableDevice: No MIDI devices found.")
                _connectionState.value = ConnectionState.NoDevice
            }
        } catch (e: SecurityException) {
            Timber.e(e, "openFirstAvailableDevice: SecurityException while getting devices.")
            _connectionState.value = ConnectionState.Error("Отсутствуют необходимые разрешения для работы с MIDI.")
        }
    }

    /**
     * Открывает соединение с указанным MIDI-устройством.
     * Перед открытием нового устройства закрывает любое ранее открытое.
     * Обрабатывает результат асинхронного вызова [MidiManager.openDevice].
     *
     * @param deviceInfo Информация о MIDI-устройстве, которое необходимо открыть.
     */
    private fun openDevice(deviceInfo: MidiDeviceInfo) {
        Timber.i("openDevice: Opening device: %s", deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME))
        if (isCurrentDevice(deviceInfo)) {
            Timber.d("openDevice: Device already open, skipping.")
            return
        }
        
        closeDevice()

        midiManager?.openDevice(deviceInfo, {
            if (it == null) {
                val deviceName = deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "Unknown Device"
                Timber.e("openDevice: Failed to open device: %s", deviceName)
                _connectionState.value = ConnectionState.Error("Не удалось подключиться к устройству: $deviceName")
                return@openDevice
            }
            Timber.i("openDevice: Device opened successfully.")
            openedDevice = it
            _connectionState.value = ConnectionState.Connected(midiDeviceMapper.toDomain(deviceInfo))
        }, null)
    }

    /**
     * Корректно закрывает текущее открытое MIDI-устройство, освобождает ресурсы
     * и обновляет состояние подключения на [ConnectionState.Disconnected].
     */
    private fun closeDevice() {
        if (openedDevice != null) {
            Timber.i("closeDevice: Closing device.")
            openedDevice?.close()
            openedDevice = null
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    private fun isCurrentDevice(device: MidiDeviceInfo): Boolean {
        return openedDevice?.info?.id == device.id
    }

    /**
     * Полностью освобождает все ресурсы, используемые [MidiDataSource].
     * Отменяет регистрацию обратного вызова и закрывает открытое устройство.
     *
     * В текущей архитектуре, где [MidiDataSource] является синглтоном (@Singleton),
     * этот метод не вызывается в обычном потоке работы приложения. Однако он предоставляет
     * необходимый механизм для явного управления ресурсами в случаях, когда
     * жизненным циклом этого компонента требуется управлять вручную.
     */
    @Suppress("unused")
    fun close() {
        Timber.i("close: Unregistering callback and closing device.")
        midiManager?.unregisterDeviceCallback(deviceCallback)
        closeDevice()
    }
}
