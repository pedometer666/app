package com.pedometer.app.domain

/**
 * Pure delta decision for raw `TYPE_STEP_COUNTER` readings. Extracted from the
 * Room transaction so it can be unit-tested on the JVM.
 *
 *  - baseline == null        -> 0  (first run adopts the raw value as baseline;
 *                                   pre-install history is intentionally ignored)
 *  - rawValue >= baseline    -> rawValue - baseline  (normal monotonic case)
 *  - rawValue < baseline     -> rawValue             (reboot / sensor reset — the
 *                                   counter restarts from zero at boot)
 */
object SensorFoldLogic {
    fun computeDeltaSteps(baseline: Long?, rawValue: Long): Long = when {
        baseline == null -> 0L
        rawValue >= baseline -> rawValue - baseline
        else -> rawValue
    }.coerceAtLeast(0L)
}
