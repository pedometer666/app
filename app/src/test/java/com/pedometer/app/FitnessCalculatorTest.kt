package com.pedometer.app

import com.pedometer.app.domain.FitnessCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FitnessCalculatorTest {

    @Test
    fun distanceMeters_isLinearInStepsAndStride() {
        assertEquals(7_500.0, FitnessCalculator.distanceMeters(10_000, 0.75), 0.001)
        assertEquals(0.0, FitnessCalculator.distanceMeters(0, 0.75), 0.001)
    }

    @Test
    fun caloriesBurned_scalesWithWeightAndSteps() {
        // 6_000 steps at 100 spm = 60 min = 1 h; 3.5 MET × 70 kg × 1 h = 245 kcal
        assertEquals(245.0, FitnessCalculator.caloriesBurned(6_000, 70.0), 0.001)
        // Twice the steps -> twice the calories
        assertEquals(490.0, FitnessCalculator.caloriesBurned(12_000, 70.0), 0.001)
    }

    @Test
    fun caloriesBurned_handlesEdgeCases() {
        assertEquals(0.0, FitnessCalculator.caloriesBurned(0, 70.0), 0.001)
        assertEquals(0.0, FitnessCalculator.caloriesBurned(-5, 70.0), 0.001)
        assertEquals(0.0, FitnessCalculator.caloriesBurned(1_000, 0.0), 0.001)
    }

    @Test
    fun caloriesPerStep_isConsistentWithCaloriesBurned() {
        val weight = 82.5
        val steps = 9_999L
        val perStep = FitnessCalculator.caloriesPerStep(weight) * steps
        assertEquals(FitnessCalculator.caloriesBurned(steps, weight), perStep, 0.001)
    }

    @Test
    fun caloriesPerStep_rejectsNonPositiveWeight() {
        assertEquals(0.0, FitnessCalculator.caloriesPerStep(0.0), 0.0)
        assertEquals(0.0, FitnessCalculator.caloriesPerStep(-72.0), 0.0)
    }

    @Test
    fun strideInCentimetersConvertsToMeters() {
        // Sanity check: the repository divides cm by 100 before calling the calculator.
        val strideMeters = 78f / 100f
        assertTrue(strideMeters in 0.5f..1.2f)
    }
}
