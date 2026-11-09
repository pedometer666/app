package com.pedometer.app.ui

/**
 * Immutable snapshot consumed by the dashboard. Everything the UI needs is
 * derived here — composables stay dumb and previewable.
 */
data class StepUiState(
    val isLoading: Boolean = true,

    // Today
    val stepsToday: Int = 0,
    val dailyGoalSteps: Int = 10_000,
    val goalProgress: Float = 0f,
    val todayDistanceMeters: Double = 0.0,
    val todayCalories: Double = 0.0,

    // Current calendar month
    val monthLabel: String = "",
    val monthSteps: Long = 0L,
    val monthDistanceMeters: Double = 0.0,
    val monthCalories: Double = 0.0,

    // Settings echoed back for the settings sheet
    val strideLengthCm: Float = 75f,
    val bodyWeightKg: Float = 70f,
    val useMetric: Boolean = true,
    val backgroundTrackingEnabled: Boolean = false,
    val isPremium: Boolean = false,

    // Live tracking state (fed by the foreground service)
    val isTracking: Boolean = false,
    val currentSpeedMps: Float = 0f
)
