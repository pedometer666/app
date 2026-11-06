package com.pedometer.app.data.local

/** Result of folding one raw sensor reading into the database transactionally. */
data class SensorFold(
    val todaySteps: Int,
    val deltaSteps: Long
)
