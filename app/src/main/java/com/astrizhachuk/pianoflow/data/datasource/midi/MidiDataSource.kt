package com.astrizhachuk.pianoflow.data.datasource.midi

import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.astrizhachuk.pianoflow.R
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
 * Является классом-одиночкой (@Singleton), так как должен существовать в единственном экземпляре на протяжении
 * всего жизненного цикла приложения, чтобы непрерывно отслеживать состояние MIDI-устройств.
 *
 * @param context Контекст приложения, необходимый для доступа к системным сервисам, таким как [MidiManager].
 * @param midiDeviceMapper Преобразователь для преобразования системной модели [MidiDeviceInfo] в доменную модель.
 * @param midiMessageParser Парсер для извлечения данных из MIDI-сообщений.
 */
class MidiDataSource @Inject constructor(
    private val context: Context,
    private val midiDeviceMapper: MidiDeviceMapper,
    private val midiMessageParser: MidiMessageParser
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
         * Запускает процесс поиска и открытия первого доступного устройства.
         */
        override fun onDeviceAdded(device: MidiDeviceInfo) {
            Timber.i("onDeviceAdded: Device detected: %s", device.deviceName(context))
            openFirstAvailableDevice()
        }

        /**
         * Вызывается системой при отключении MIDI-устройства.
         * Если отключенное устройство является текущим открытым устройством, закрывает его.
         */
        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            Timber.w("onDeviceRemoved: Device disconnected: %s", device.deviceName(context))
            if (isCurrentDevice(device)) {
                Timber.d("onDeviceRemoved: Disconnected device is current, closing.")
                closeDevice()
            }
        }
    }

    init {
        Timber.i("init: Initializing.")
        when {
            !context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI) -> {
                Timber.w("init: MIDI feature NOT supported.")
                _connectionState.value = ConnectionState.Error(context.getString(R.string.midi_error_api_unsupported))
            }
            midiManager == null -> {
                Timber.w("init: MidiManager is null, MIDI system service not available.")
                _connectionState.value = ConnectionState.NoDevice
            }
            else -> {
                Timber.i("init: MIDI feature supported.")
                midiManager.registerDeviceCallbackCompat(deviceCallback, Handler(Looper.getMainLooper()))
                openFirstAvailableDevice()
            }
        }
    }

    /**
     * Полностью освобождает все ресурсы, используемые [MidiDataSource].
     * Отменяет регистрацию обратного вызова и закрывает открытое устройство.
     *
     * В текущей архитектуре, где [MidiDataSource] является классом-одиночкой (@Singleton),
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

    /**
     * Ищет первое доступное MIDI-устройство в системе и инициирует его открытие.
     * Если устройства не найдены, устанавливает состояние в [ConnectionState.NoDevice].
     * Безопасно обрабатывает SecurityException, если у приложения нет разрешений.
     */
    private fun openFirstAvailableDevice() {
        Timber.d("openFirstAvailableDevice: Looking for devices.")
        try {
            midiManager!!.getFirstAvailableDevice()?.let { device ->
                Timber.i("openFirstAvailableDevice: Found device: %s. Attempting to open.", device.deviceName(context))
                openDevice(device)
            } ?: run {
                Timber.i("openFirstAvailableDevice: No MIDI devices found.")
                _connectionState.value = ConnectionState.NoDevice
            }
        } catch (e: SecurityException) {
            Timber.e(e, "openFirstAvailableDevice: SecurityException while getting devices.")
            _connectionState.value = ConnectionState.Error(context.getString(R.string.midi_error_no_permissions))
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
        Timber.i("openDevice: Opening device: %s", deviceInfo.deviceName(context))
        if (isCurrentDevice(deviceInfo)) {
            Timber.d("openDevice: Device already open, skipping.")
            return
        }
        
        closeDevice()

        midiManager!!.openDevice(deviceInfo, {
            if (it == null) {
                val deviceName = deviceInfo.deviceName(context)
                Timber.w("openDevice: Failed to open device: %s", deviceName)
                _connectionState.value = ConnectionState.Error(
                    context.getString(R.string.midi_error_connection_failed, deviceName)
                )
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
        openedDevice?.let {
            Timber.i("closeDevice: Closing device.")
            it.close()
            openedDevice = null
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    /**
     * Проверяет, является ли предоставленное устройство [device] текущим открытым устройством.
     *
     * @param device Устройство для проверки.
     * @return `true`, если ID предоставленного устройства совпадает с ID текущего открытого устройства, иначе `false`.
     */
    private fun isCurrentDevice(device: MidiDeviceInfo): Boolean {
        return openedDevice?.info?.id == device.id
    }
}

/**
 * Регистрирует обратный вызов для отслеживания MIDI-устройств,
 * используя подходящий API в зависимости от версии Android и обеспечивая выполнение в одном потоке.
 *
 * Для Android 13 (API 33) и выше используется [MidiManager.registerDeviceCallback]
 * с указанием транспорта [MidiManager.TRANSPORT_MIDI_BYTE_STREAM]. Предоставленный [handler]
 * адаптируется в `Executor` для выполнения колбэка в нужном потоке.
 * Для более старых версий используется устаревший метод [MidiManager.registerDeviceCallback],
 * которому [handler] передается напрямую.
 *
 * @param callback Обратный вызов для событий устройств.
 * @param handler Handler, в потоке которого будут выполняться обратные вызовы.
 */
private fun MidiManager.registerDeviceCallbackCompat(
    callback: MidiManager.DeviceCallback,
    handler: Handler
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerDeviceCallback(
            MidiManager.TRANSPORT_MIDI_BYTE_STREAM,
            handler::post,
            callback
        )
    } else {
        @Suppress("DEPRECATION")
        registerDeviceCallback(callback, handler)
    }
}

/**
 * Возвращает первое доступное MIDI-устройство, используя подходящий API в зависимости от версии Android.
 *
 * Для Android 13 (API 33) и выше используется [MidiManager.getDevicesForTransport]
 * для получения устройств, подключенных через [MidiManager.TRANSPORT_MIDI_BYTE_STREAM].
 * Для более старых версий используется устаревший метод [MidiManager.getDevices].
 */
private fun MidiManager.getFirstAvailableDevice(): MidiDeviceInfo? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM).firstOrNull()
    } else {
        @Suppress("DEPRECATION")
        devices.firstOrNull()
    }
}

/**
 * Вспомогательное свойство-расширение для безопасного получения имени [MidiDeviceInfo].
 *
 * Возвращает имя устройства из его свойств или `context.getString(R.string.midi_unknown_device)`, если имя отсутствует.
 */
private fun MidiDeviceInfo.deviceName(context: Context): String {
    return properties.getString(MidiDeviceInfo.PROPERTY_NAME)
        ?: context.getString(R.string.midi_unknown_device)
}
