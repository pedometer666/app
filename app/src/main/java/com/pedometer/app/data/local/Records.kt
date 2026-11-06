package com.pedometer.app.data.local

/** DAO projections for the Homebase screen. Property names match SQL aliases. */

data class BestDayRecord(
    val date: String,
    val steps: Int
)

data class BestDistanceRecord(
    val date: String,
    val distanceMeters: Double
)

data class LifetimeTotals(
    val totalSteps: Long,
    val totalCalories: Double,
    val totalDistanceMeters: Double,
    val activeDays: Long
)
