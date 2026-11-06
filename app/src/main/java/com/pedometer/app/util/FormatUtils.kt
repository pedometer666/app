package com.pedometer.app.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/** Display formatting helpers. All functions are locale-aware. */
object FormatUtils {

    private const val METERS_PER_KILOMETER = 1_000.0
    private const val METERS_PER_MILE = 1_609.344

    fun formatSteps(value: Long): String =
        NumberFormat.getIntegerInstance().format(value)

    fun formatCalories(kcal: Double): String =
        NumberFormat.getIntegerInstance().format(kcal.roundToInt())

    fun formatDistanceValue(meters: Double, useMetric: Boolean): String {
        val distance = if (useMetric) meters / METERS_PER_KILOMETER else meters / METERS_PER_MILE
        return String.format(Locale.getDefault(), "%.2f", distance)
    }

    fun distanceUnit(useMetric: Boolean): String = if (useMetric) "km" else "mi"

    fun formatDistance(meters: Double, useMetric: Boolean): String =
        "${formatDistanceValue(meters, useMetric)} ${distanceUnit(useMetric)}"

    /** Converts m/s into the display unit used by the speedometer. */
    fun speedValue(metersPerSecond: Float, useMetric: Boolean): Float =
        if (useMetric) metersPerSecond * 3.6f else metersPerSecond * 2.236_936_3f

    fun speedUnit(useMetric: Boolean): String = if (useMetric) "km/h" else "mph"

    fun formatSpeed(metersPerSecond: Float, useMetric: Boolean): String =
        String.format(Locale.getDefault(), "%.1f", speedValue(metersPerSecond, useMetric))

    fun formatCentimetersAsInches(cm: Float): Float = cm / 2.54f

    fun formatKilogramsAsPounds(kg: Float): Float = kg * 2.204_622_6f
}
