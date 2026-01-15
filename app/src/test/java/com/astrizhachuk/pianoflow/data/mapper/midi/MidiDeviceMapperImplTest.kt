package com.astrizhachuk.pianoflow.data.mapper.midi

import android.media.midi.MidiDeviceInfo
import android.os.Bundle
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
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
            putString(MidiDeviceInfo.PROPERTY_PRODUCT, "The Grand")
        }
        val deviceInfo = mockk<MidiDeviceInfo>(relaxed = true)
        every { deviceInfo.properties } returns properties

        // Act
        val domainDevice = mapper.toDomain(deviceInfo)

        // Assert
        assertEquals("Digital Piano", domainDevice.name)
        assertEquals("PianoCorp", domainDevice.manufacturer)
        assertEquals("The Grand", domainDevice.product)
    }

    @Test
    fun `toDomain should use empty string when name and product are missing`() {
        // Arrange
        val properties = Bundle().apply {
            putString(MidiDeviceInfo.PROPERTY_MANUFACTURER, "PianoCorp")
        }
        val deviceInfo = mockk<MidiDeviceInfo>(relaxed = true)
        every { deviceInfo.properties } returns properties

        // Act
        val domainDevice = mapper.toDomain(deviceInfo)

        // Assert
        assertEquals("", domainDevice.name)
        assertEquals("PianoCorp", domainDevice.manufacturer)
        assertEquals("", domainDevice.product)
    }

    @Test
    fun `toDomain should use empty string when manufacturer and product are missing`() {
        // Arrange
        val properties = Bundle().apply {
            putString(MidiDeviceInfo.PROPERTY_NAME, "Digital Piano")
        }
        val deviceInfo = mockk<MidiDeviceInfo>(relaxed = true)
        every { deviceInfo.properties } returns properties

        // Act
        val domainDevice = mapper.toDomain(deviceInfo)

        // Assert
        assertEquals("Digital Piano", domainDevice.name)
        assertEquals("", domainDevice.manufacturer)
        assertEquals("", domainDevice.product)
    }

    @Test
    fun `toDomain should use empty string when name and manufacturer are missing`() {
        // Arrange
        val properties = Bundle().apply {
            putString(MidiDeviceInfo.PROPERTY_PRODUCT, "The Grand")
        }
        val deviceInfo = mockk<MidiDeviceInfo>(relaxed = true)
        every { deviceInfo.properties } returns properties

        // Act
        val domainDevice = mapper.toDomain(deviceInfo)

        // Assert
        assertEquals("", domainDevice.name)
        assertEquals("", domainDevice.manufacturer)
        assertEquals("The Grand", domainDevice.product)
    }

    @Test
    fun `toDomain should use empty strings when properties bundle is empty`() {
        // Arrange
        val deviceInfo = mockk<MidiDeviceInfo>(relaxed = true)
        every { deviceInfo.properties } returns Bundle()

        // Act
        val domainDevice = mapper.toDomain(deviceInfo)

        // Assert
        assertEquals("", domainDevice.name)
        assertEquals("", domainDevice.manufacturer)
        assertEquals("", domainDevice.product)
    }

    @Test
    fun `toDomain should use empty strings for null property values`() {
        // Arrange
        val properties = Bundle().apply {
            putString(MidiDeviceInfo.PROPERTY_NAME, null)
            putString(MidiDeviceInfo.PROPERTY_MANUFACTURER, null)
            putString(MidiDeviceInfo.PROPERTY_PRODUCT, null)
        }
        val deviceInfo = mockk<MidiDeviceInfo>(relaxed = true)
        every { deviceInfo.properties } returns properties

        // Act
        val domainDevice = mapper.toDomain(deviceInfo)

        // Assert
        assertEquals("", domainDevice.name)
        assertEquals("", domainDevice.manufacturer)
        assertEquals("", domainDevice.product)
    }

    @Test
    fun `toDomain should throw NPE on null properties bundle`() {
        // Arrange
        val deviceInfo = mockk<MidiDeviceInfo>(relaxed = true)
        every { deviceInfo.properties } returns null

        // Act & Assert
        try {
            mapper.toDomain(deviceInfo)
            fail("Expected NullPointerException was not thrown.")
        } catch (e: NullPointerException) {
            // Success, the expected exception was caught.
        }
    }
}
