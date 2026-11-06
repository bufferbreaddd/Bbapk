package dev.colorup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimationConfigTest {
    @Test
    fun defaultsContainStableIndependentTransitionIds() {
        val ids = AnimationCatalog.defaults.map { it.id }
        assertEquals(18, ids.size)
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.contains("unlock_to_home"))
        assertTrue(ids.contains("shade_open"))
        assertTrue(ids.contains("notification_dismiss"))
        assertTrue(AnimationCatalog.defaults.none { it.enabled })
    }

    @Test
    fun presetChangesAreIndependentCopies() {
        val fluid = AnimationCatalog.preset("Fluid")
        assertTrue(fluid.first { it.id == "unlock_to_app" }.enabled)
        assertTrue(fluid.first { it.id == "shade_open" }.enabled)
        assertTrue(fluid.first { it.id == "shade_close" }.enabled)
        assertTrue(fluid.first { it.id == "qs_expand" }.enabled)
        assertTrue(fluid.first { it.id == "qs_collapse" }.enabled)
        assertTrue(fluid.first { it.id == "notification_appear" }.enabled)
        val implemented = AnimationCatalog.experimentalHookIds
        assertTrue(fluid.filter { it.id !in implemented }.none { it.enabled })
        fluid.first().durationMs = 999
        assertFalse(AnimationCatalog.defaults.first().durationMs == 999)
    }

    @Test
    fun jsonRoundTripPreservesPerTransitionValues() {
        val source = AnimationCatalog.preset("Fluid")
        source.first().delayMs = 180
        source.first().direction = "LEFT"
        source.first().springStiffness = 720f
        val decoded = ConfigCodec.decode(ConfigCodec.encode(source))
        assertEquals(180, decoded.first().delayMs)
        assertEquals("LEFT", decoded.first().direction)
        assertEquals(720f, decoded.first().springStiffness)
        assertTrue(decoded.first { it.id == "unlock_to_app" }.enabled)
    }

    @Test
    fun importedValuesAreClampedAndUnknownIdsIgnored() {
        val json = """{"schemaVersion":2,"animations":[{"id":"keyguard_show","enabled":true,"durationMs":9000,"scale":9.0,"fade":-2.0,"translateY":999,"delayMs":-1,"curve":"NOT_A_CURVE","direction":"SIDEWAYS"},{"id":"not_real","enabled":true}]}"""
        val item = ConfigCodec.decode(json).first { it.id == "keyguard_show" }
        assertEquals(2000, item.durationMs)
        assertEquals(2f, item.scale)
        assertEquals(0f, item.fade)
        assertEquals(150, item.translateY)
        assertEquals(0, item.delayMs)
        assertEquals("STANDARD", item.curve)
        assertEquals("UP", item.direction)
        assertFalse(item.enabled)
    }

    @Test
    fun panelMotionCurvesStayWithinSafeBounds() {
        assertEquals(0.5f, MotionMath.ease(0.5f, "LINEAR", 0.8f, 420f), 0.0001f)
        assertEquals(0.25f, MotionMath.ease(0.5f, "EASE_IN", 0.8f, 420f), 0.0001f)
        assertEquals(0.75f, MotionMath.ease(0.5f, "EASE_OUT", 0.8f, 420f), 0.0001f)
        assertEquals(0.5f, MotionMath.ease(0.5f, "EASE_IN_OUT", 0.8f, 420f), 0.0001f)
        assertEquals(0f, MotionMath.ease(0f, "SPRING", 0.8f, 420f), 0.0001f)
        for (i in 0..100) {
            val value = MotionMath.ease(i / 100f, "SPRING", 0.8f, 720f)
            assertTrue(value in 0f..1.15f)
        }
    }
}
