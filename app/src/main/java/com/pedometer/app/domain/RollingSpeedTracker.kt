package com.pedometer.app.domain

import java.util.ArrayDeque

/**
 * Computes a smooth live speed from a rolling time window of step samples.
 *
 * The step counter produces irregular, batched events, so a single inter-event
 * delta is jittery. Instead we keep every sample inside a sliding window
 * (default 30 s) and derive speed from the first-to-last span in the window.
 *
 * Timestamps are injected by the caller, which keeps this class deterministic
 * and unit-testable on the JVM.
 */
class RollingSpeedTracker(private val windowMillis: Long = DEFAULT_WINDOW_MILLIS) {

    private data class Sample(val timestampMillis: Long, val cumulativeSteps: Long)

    private val samples = ArrayDeque<Sample>()

    /** Steps counted per second across the retained window, or null before enough data exists. */
    fun addSample(timestampMillis: Long, cumulativeSteps: Long): Double? {
        samples.addLast(Sample(timestampMillis, cumulativeSteps))
        prune(timestampMillis)
        return stepsPerSecond()
    }

    /**
     * Steps/second between the oldest retained sample and now.
     * Returns 0.0 when the window spans at least [MIN_SPAN_MILLIS] but no steps were taken,
     * and null when there is not enough data yet to derive a cadence.
     */
    fun stepsPerSecond(): Double? {
        if (samples.size < 2) return null
        val oldest = samples.first()
        val newest = samples.last()
        val spanMillis = newest.timestampMillis - oldest.timestampMillis
        if (spanMillis < MIN_SPAN_MILLIS) return null
        val stepDelta = newest.cumulativeSteps - oldest.cumulativeSteps
        return if (stepDelta <= 0L) 0.0 else stepDelta / (spanMillis / 1_000.0)
    }

    /** Live speed in m/s, combining the windowed cadence with stride length. */
    fun speedMetersPerSecond(strideLengthMeters: Double): Double =
        (stepsPerSecond() ?: 0.0) * strideLengthMeters

    fun clear() = samples.clear()

    private fun prune(nowMillis: Long) {
        val cutoff = nowMillis - windowMillis
        // Keep the newest sample at/behind the cutoff as the window boundary so the
        // effective span stays close to [windowMillis] instead of collapsing to 0.
        while (samples.size >= 2 && samples.elementAt(1).timestampMillis <= cutoff) {
            samples.removeFirst()
        }
    }

    companion object {
        const val DEFAULT_WINDOW_MILLIS: Long = 30_000L
        private const val MIN_SPAN_MILLIS: Long = 3_000L
    }
}
