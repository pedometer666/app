package com.pedometer.app

import com.pedometer.app.domain.SensorFoldLogic
import org.junit.Assert.assertEquals
import org.junit.Test

class SensorFoldLogicTest {

    @Test
    fun `first reading adopts baseline and counts nothing`() {
        assertEquals(0L, SensorFoldLogic.computeDeltaSteps(baseline = null, rawValue = 12_345L))
    }

    @Test
    fun `monotonic reading counts the delta`() {
        assertEquals(25L, SensorFoldLogic.computeDeltaSteps(baseline = 1_000L, rawValue = 1_025L))
    }

    @Test
    fun `equal reading counts nothing`() {
        assertEquals(0L, SensorFoldLogic.computeDeltaSteps(baseline = 1_000L, rawValue = 1_000L))
    }

    @Test
    fun `counter reset after reboot counts everything since boot`() {
        // Raw value went backwards: device rebooted, counter restarted at 0.
        assertEquals(320L, SensorFoldLogic.computeDeltaSteps(baseline = 900_000L, rawValue = 320L))
    }

    @Test
    fun `zero raw after reboot counts nothing`() {
        assertEquals(0L, SensorFoldLogic.computeDeltaSteps(baseline = 900_000L, rawValue = 0L))
    }

    @Test
    fun `never returns a negative delta`() {
        assertEquals(0L, SensorFoldLogic.computeDeltaSteps(baseline = -5L, rawValue = -10L))
    }
}
