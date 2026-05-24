package com.astrizhachuk.pianoflow.domain.service.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordTypeRegistryTest {

    @Test
    fun `registry has exactly 106 entries`() {
        assertEquals(106, ChordTypeRegistry.all.size)
    }

    @Test
    fun `every chord type chroma has bit 0 set (root always present)`() {
        ChordTypeRegistry.all.forEach { ct ->
            assertTrue("${ct.symbol} chroma=${ct.chroma} missing bit 0", ct.chroma and 1 == 1)
        }
    }

    @Test
    fun `every chord type chroma fits in 12 bits`() {
        ChordTypeRegistry.all.forEach { ct ->
            assertTrue("${ct.symbol} chroma=${ct.chroma} exceeds 12 bits", ct.chroma < 4096)
        }
    }

    @Test
    fun `lookup index covers all 106 entries`() {
        val total = ChordTypeRegistry.byChroma.values.sumOf { it.size }
        assertEquals(106, total)
    }

    @Test
    fun `spot-check M (major) entry exists`() {
        val found = ChordTypeRegistry.all.firstOrNull { it.symbol == "M" }
        assertNotNull("chord type 'M' not found", found)
    }

    @Test
    fun `spot-check m (minor) entry exists`() {
        val found = ChordTypeRegistry.all.firstOrNull { it.symbol == "m" }
        assertNotNull("chord type 'm' not found", found)
    }

    @Test
    fun `spot-check 7 (dominant seventh) entry exists`() {
        val found = ChordTypeRegistry.all.firstOrNull { it.symbol == "7" }
        assertNotNull("chord type '7' not found", found)
    }

    @Test
    fun `spot-check m7 (minor seventh) entry exists`() {
        val found = ChordTypeRegistry.all.firstOrNull { it.symbol == "m7" }
        assertNotNull("chord type 'm7' not found", found)
    }

    @Test
    fun `spot-check dim (diminished) entry exists`() {
        val found = ChordTypeRegistry.all.firstOrNull { it.symbol == "dim" }
        assertNotNull("chord type 'dim' not found", found)
    }

    @Test
    fun `spot-check aug (augmented) entry exists`() {
        val found = ChordTypeRegistry.all.firstOrNull { it.symbol == "aug" }
        assertNotNull("chord type 'aug' not found", found)
    }

    @Test
    fun `byChroma lookup for M returns correct bitmask`() {
        // C major = [C,E,G] = bits 0,4,7 = 1+16+128 = 145
        val types = ChordTypeRegistry.byChroma[145]
        assertNotNull(types)
        assertTrue(types!!.any { it.symbol == "M" })
    }

    @Test
    fun `byChroma lookup for m returns correct bitmask`() {
        // minor = [C,Eb,G] = bits 0,3,7 = 1+8+128 = 137
        val types = ChordTypeRegistry.byChroma[137]
        assertNotNull(types)
        assertTrue(types!!.any { it.symbol == "m" })
    }

    @Test
    fun `collision chroma 100010001001 holds M7b6 and maj7#5`() {
        // "100010001001" reversed = "100100010001" -> bits 0,4,8,11 = 1+16+256+2048 = 2321
        val types = ChordTypeRegistry.byChroma[2321]
        assertNotNull(types)
        assertEquals(2, types!!.size)
        assertTrue(types.any { it.symbol == "M7b6" })
        assertTrue(types.any { it.symbol == "maj7#5" })
    }
}
