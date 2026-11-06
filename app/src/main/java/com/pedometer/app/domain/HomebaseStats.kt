package com.pedometer.app.domain

/** One bar in the trailing-7-day mini chart. */
data class DayStepValue(val date: String, val steps: Int)

/**
 * Aggregated analytics consumed by the Homebase screen.
 * All distance fields are meters; percentages are signed percent deltas
 * (null when the previous period had no data to compare against).
 */
data class HomebaseStats(
    val weeklyAverageSteps: Double,
    val previousWeekAverageSteps: Double,
    val weekOverWeekPercent: Float?,
    val last7Days: List<DayStepValue>,

    val monthToDateSteps: Long,
    val monthToDateDistanceMeters: Double,
    val monthToDateCalories: Double,
    val previousMonthToDateSteps: Long,
    val previousMonthToDateDistanceMeters: Double,
    val monthDeltaPercent: Float?,

    val bestDaySteps: Int,
    val bestDayDate: String?,
    val bestDayDistanceMeters: Double,
    val bestDayDistanceDate: String?,

    val lifetimeSteps: Long,
    val lifetimeDistanceMeters: Double,
    val lifetimeActiveDays: Long,

    val currentStreakDays: Int
) {
    companion object {
        val EMPTY = HomebaseStats(
            weeklyAverageSteps = 0.0,
            previousWeekAverageSteps = 0.0,
            weekOverWeekPercent = null,
            last7Days = emptyList(),
            monthToDateSteps = 0L,
            monthToDateDistanceMeters = 0.0,
            monthToDateCalories = 0.0,
            previousMonthToDateSteps = 0L,
            previousMonthToDateDistanceMeters = 0.0,
            monthDeltaPercent = null,
            bestDaySteps = 0,
            bestDayDate = null,
            bestDayDistanceMeters = 0.0,
            bestDayDistanceDate = null,
            lifetimeSteps = 0L,
            lifetimeDistanceMeters = 0.0,
            lifetimeActiveDays = 0L,
            currentStreakDays = 0
        )
    }
}
