package com.pedometer.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per calendar day, keyed by the ISO date `YYYY-MM-DD`.
 *
 * Distance is stored in meters and calories in kcal. Both are derived metrics
 * computed at write time from the user's stride length and body weight; they
 * are recomputed for all rows when the user changes those settings
 * (see [com.pedometer.app.data.local.StepDao.recomputeDerivedMetrics]).
 */
@Entity(tableName = "steps")
data class StepEntity(
    @PrimaryKey val date: String,
    val steps: Int = 0,
    val calories: Double = 0.0,
    val distanceMeters: Double = 0.0,
    /** Epoch millis of the last sensor write — useful for debugging/ordering. */
    val updatedAt: Long = 0L
)
