package com.pedometer.app.data.local

/**
 * Query projection returned by [StepDao.observeMonthlyTotals].
 * Property names must match the `AS` aliases in the SQL query.
 */
data class MonthlyTotals(
    val totalSteps: Long,
    val totalCalories: Double,
    val totalDistanceMeters: Double
) {
    companion object {
        val EMPTY = MonthlyTotals(totalSteps = 0L, totalCalories = 0.0, totalDistanceMeters = 0.0)
    }
}
