package com.pedometer.app.domain

/**
 * Pure, side-effect-free fitness math. Kept dependency-free so it can be unit
 * tested on the JVM without an Android runtime.
 */
object FitnessCalculator {

    /**
     * Metabolic equivalent for moderate-pace walking (~5 km/h).
     * Reference: Compendium of Physical Activities (walking, 3.0–3.5 mph).
     */
    const val WALKING_MET: Double = 3.5

    /** Assumed average walking cadence used to derive active time from steps. */
    const val AVERAGE_CADENCE_STEPS_PER_MINUTE: Double = 100.0

    /** Stride length the app ships with until the user personalises it (75 cm). */
    const val DEFAULT_STRIDE_LENGTH_CM: Float = 75f

    /** Weight default (70 kg). */
    const val DEFAULT_BODY_WEIGHT_KG: Float = 70f

    /**
     * Distance covered by [steps] steps at the given [strideLengthMeters].
     * This is deliberately a direct linear conversion — stride length is the
     * only distance input the user provides.
     */
    fun distanceMeters(steps: Long, strideLengthMeters: Double): Double =
        steps * strideLengthMeters

    /**
     * Active calories burned by [steps] steps for a person of [weightKg].
     *
     * kcal = MET × weight(kg) × hours, where active hours are derived from
     * step volume and the average walking cadence:
     *   hours = steps / (cadence × 60)
     */
    fun caloriesBurned(steps: Long, weightKg: Double): Double {
        if (steps <= 0L || weightKg <= 0.0) return 0.0
        val minutes = steps / AVERAGE_CADENCE_STEPS_PER_MINUTE
        return WALKING_MET * weightKg * (minutes / 60.0)
    }

    /** Calories burned per step — scalar used to increment rows incrementally. */
    fun caloriesPerStep(weightKg: Double): Double =
        if (weightKg <= 0.0) 0.0 else WALKING_MET * weightKg / (AVERAGE_CADENCE_STEPS_PER_MINUTE * 60.0)
}
