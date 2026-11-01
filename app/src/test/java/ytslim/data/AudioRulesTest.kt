package ytslim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioRulesTest {
    @Test
    fun recognizesOriginalLabelsAsTheEngineWritesThem() {
        assertTrue(AudioRules.isOriginal("en‐US (original)"))
        // O Media3 passa o idioma para minúsculas.
        assertTrue(AudioRules.isOriginal("en‐us (original)"))
        assertFalse(AudioRules.isOriginal("pt‐BR (dubbed-auto)"))
        assertFalse(AudioRules.isOriginal("es (dubbed)"))
        assertFalse(AudioRules.isOriginal(null))
    }

    @Test
    fun picksTheOriginalGroupEvenWhenItIsNotFirst() {
        val groups = listOf("pt‐br (dubbed-auto)", "es (dubbed)", "en‐us (original)", "en‐us (descriptive)")
        assertEquals(2, AudioRules.pickOriginalGroup(groups))
    }

    @Test
    fun leavesSingleTrackVideosAlone() {
        assertNull(AudioRules.pickOriginalGroup(listOf(null)))
        assertNull(AudioRules.pickOriginalGroup(emptyList()))
        assertNull(AudioRules.originalLabel(listOf("en")))
    }
}
