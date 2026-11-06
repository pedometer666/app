package com.pedometer.app

import com.pedometer.app.domain.RollingSpeedTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RollingSpeedTrackerTest {

    @Test
    fun stepsPerSecond_requiresTwoSamplesAndMinimumSpan() {
        val tracker = RollingSpeedTracker(windowMillis = 30_000)
        assertNull(tracker.stepsPerSecond())

        tracker.addSample(timestampMillis = 0, cumulativeSteps = 100)
        // Span below MIN_SPAN_MILLIS (3s) -> not enough data yet
        tracker.addSample(timestampMillis = 1_000, cumulativeSteps = 102)
        assertNull(tracker.stepsPerSecond())
    }

    @Test
    fun stepsPerSecond_computesCadenceAcrossWindow() {
        val tracker = RollingSpeedTracker(windowMillis = 30_000)
        tracker.addSample(timestampMillis = 0, cumulativeSteps = 0)
        tracker.addSample(timestampMillis = 10_000, cumulativeSteps = 14)

        assertEquals(1.4, tracker.stepsPerSecond()!!, 0.0001)
    }

    @Test
    fun speedMetersPerSecond_usesStrideLength() {
        val tracker = RollingSpeedTracker(windowMillis = 30_000)
        tracker.addSample(timestampMillis = 0, cumulativeSteps = 0)
        tracker.addSample(timestampMillis = 10_000, cumulativeSteps = 20)

        assertEquals(1.4, tracker.stepsPerSecond()!!, 0.0001)
        assertEquals(1.4 * 0.75, tracker.speedMetersPerSecond(0.75), 0.0001)
    }

    @Test
    fun stationaryWindow_reportsZeroNotNull() {
        val tracker = RollingSpeedTracker(windowMillis = 30_000)
        tracker.addSample(timestampMillis = 0, cumulativeSteps = 500)
        tracker.addSample(timestampMillis = 10_000, cumulativeSteps = 500)

        assertEquals(0.0, tracker.stepsPerSecond()!!, 0.0)
        assertEquals(0.0, tracker.speedMetersPerSecond(0.75), 0.0)
    }

    @Test
    fun oldSamples_arePrunedButBoundaryIsRetained() {
        val tracker = RollingSpeedTracker(windowMillis = 30_000)
        tracker.addSample(timestampMillis = 0, cumulativeSteps = 0)
        tracker.addSample(timestampMillis = 10_000, cumulativeSteps = 10)
        // Jump far ahead; the boundary sample is kept so the window stays meaningful.
        tracker.addSample(timestampMillis = 40_000, cumulativeSteps = 40)

        assertNotNull(tracker.stepsPerSecond())
        assertEquals(1.0, tracker.stepsPerSecond()!!, 0.0001)
    }

    @Test
    fun clear_resetsState() {
        val tracker = RollingSpeedTracker()
        tracker.addSample(timestampMillis = 0, cumulativeSteps = 0)
        tracker.addSample(timestampMillis = 10_000, cumulativeSteps = 100)
        tracker.clear()
        assertNull(tracker.stepsPerSecond())
    }
}
