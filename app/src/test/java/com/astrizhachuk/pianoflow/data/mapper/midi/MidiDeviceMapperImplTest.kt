
package com.astrizhachuk.pianoflow.data.mapper.midi

import android.media.midi.MidiDeviceInfo
import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MidiDeviceMapperImplTest {

    private val mapper = MidiDeviceMapperImpl()

    @Test
    fun `toDomain should correctly map all properties`() {
        // Arrange
        val properties = Bundle().apply {
            putString(MidiDeviceInfo.PROPERTY_NAME, "Digital Piano")
            putString(MidiDeviceInfo.PROPERTY_MANUFACTURER, "PianoCorp")
        }
        val deviceInfo = mock<MidiDeviceInfo>()
        whenever(deviceInfo.properties).thenReturn(properties)

        // Act
        val domainDevice = mapper.toDomain(deviceInfo)

        // Assert
        assertEquals("Digital Piano", domainDevice.name)
        assertEquals("PianoCorp", domainDevice.vendor)
    }

    @Test
    fun `toDomain should handle missing product name`() {
        // Arrange
        val properties = Bundle().apply {
            putString(MidiDeviceInfo.PROPERTY_MANUFACTURER, "PianoCorp")
        }
        val deviceInfo = mock<MidiDeviceInfo>()
        whenever(deviceInfo.properties).thenReturn(properties)

        // Act
        val domainDevice = mapper.toDomain(deviceInfo)

        // Assert
        assertEquals("", domainDevice.name)
        assertEquals("PianoCorp", domainDevice.vendor)
    }

    @Test
    fun `toDomain should handle missing manufacturer name`() {
        // Arrange
        val properties = Bundle().apply {
            putString(MidiDeviceInfo.PROPERTY_NAME, "Digital Piano")
        }
        val deviceInfo = mock<MidiDeviceInfo>()
        whenever(deviceInfo.properties).thenReturn(properties)

        // Act
        val domainDevice = mapper.toDomain(deviceInfo)

        // Assert
        assertEquals("Digital Piano", domainDevice.name)
        assertEquals("", domainDevice.vendor)
    }

    @Test
    fun `toDomain should handle empty properties bundle`() {
        // Arrange
        val deviceInfo = mock<MidiDeviceInfo>()
        whenever(deviceInfo.properties).thenReturn(Bundle())

        // Act
        val domainDevice = mapper.toDomain(deviceInfo)

        // Assert
        assertEquals("", domainDevice.name)
        assertEquals("", domainDevice.vendor)
    }

    @Test
    fun `toDomain should throw NPE on null properties bundle`() {
        // Arrange
        val deviceInfo = mock<MidiDeviceInfo>()
        whenever(deviceInfo.properties).thenReturn(null)

        // Act & Assert
        try {
            mapper.toDomain(deviceInfo)
            fail("Expected NullPointerException was not thrown.")
        } catch (e: NullPointerException) {
            // Success, the expected exception was caught.
        }
    }
}
