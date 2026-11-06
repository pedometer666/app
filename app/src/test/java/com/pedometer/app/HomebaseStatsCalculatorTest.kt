package com.pedometer.app

import com.pedometer.app.data.local.BestDayRecord
import com.pedometer.app.data.local.BestDistanceRecord
import com.pedometer.app.data.local.LifetimeTotals
import com.pedometer.app.data.local.MonthlyTotals
import com.pedometer.app.data.local.StepEntity
import com.pedometer.app.domain.HomebaseStatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class HomebaseStatsCalculatorTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 5)

    private fun entity(daysAgo: Long, steps: Int, distanceMeters: Double = steps * 0.75) =
        StepEntity(
            date = today.minusDays(daysAgo).toString(),
            steps = steps,
            calories = steps * 0.04,
            distanceMeters = distanceMeters,
            updatedAt = 0L
        )

    @Test
    fun weeklyAverage_dividesBySevenIncludingMissingDays() {
        val trailing14 = listOf(
            entity(0, 3_500),
            entity(1, 7_000),
            // days 2..6 missing -> zeros
            entity(7, 14_000) // previous week only
        )
        val stats = HomebaseStatsCalculator.build(
            today = today,
            trailing14Days = trailing14,
            streakWindow = trailing14,
            monthToDate = MonthlyTotals(10_500, 420.0, 7_875.0),
            previousMonthToDate = MonthlyTotals(7_000, 280.0, 5_250.0),
            bestStepDay = BestDayRecord(today.toString(), 7_000),
            bestDistanceDay = BestDistanceRecord(today.toString(), 5_250.0),
            lifetime = LifetimeTotals(24_500, 980.0, 18_375.0, 3),
            dailyGoal = 10_000
        )
        assertEquals(1_500.0, stats.weeklyAverageSteps, 0.001)  // 10_500 / 7
        assertEquals(2_000.0, stats.previousWeekAverageSteps, 0.001) // 14_000 / 7
        assertEquals(-25f, stats.weekOverWeekPercent!!, 0.01f)
        assertEquals(7, stats.last7Days.size)
        assertEquals(3_500, stats.last7Days.last().steps) // today is the last bar
    }

    @Test
    fun monthDeltaPercent_isNullWithoutBaseline() {
        val stats = HomebaseStatsCalculator.build(
            today = today,
            trailing14Days = emptyList(),
            streakWindow = emptyList(),
            monthToDate = MonthlyTotals(5_000, 0.0, 0.0),
            previousMonthToDate = MonthlyTotals(0, 0.0, 0.0),
            bestStepDay = null,
            bestDistanceDay = null,
            lifetime = LifetimeTotals(5_000, 0.0, 0.0, 1),
            dailyGoal = 10_000
        )
        assertNull(stats.monthDeltaPercent)
    }

    @Test
    fun streak_countsConsecutiveGoalDaysEndingYesterday_todayInProgress() {
        val window = listOf(
            entity(0, 100),     // today, below goal — must not break the streak
            entity(1, 12_000),
            entity(2, 10_500),
            entity(3, 30_000),
            entity(4, 9_999)    // below goal — streak ends
        )
        assertEquals(3, HomebaseStatsCalculator.currentStreak(today, window, 10_000))
    }

    @Test
    fun streak_includesTodayWhenGoalReached() {
        val window = listOf(
            entity(0, 11_000),
            entity(1, 10_100)
        )
        assertEquals(2, HomebaseStatsCalculator.currentStreak(today, window, 10_000))
    }

    @Test
    fun streak_zeroWhenNothingMeetsGoal() {
        val window = listOf(entity(0, 100), entity(1, 200))
        assertEquals(0, HomebaseStatsCalculator.currentStreak(today, window, 10_000))
    }

    @Test
    fun last7Days_areOrderedOldestToNewest() {
        val window = (0L..6L).map { entity(it, (it + 1).toInt() * 1_000) }
        val stats = HomebaseStatsCalculator.build(
            today = today,
            trailing14Days = window,
            streakWindow = window,
            monthToDate = MonthlyTotals(28_000, 0.0, 0.0),
            previousMonthToDate = MonthlyTotals(28_000, 0.0, 0.0),
            bestStepDay = null,
            bestDistanceDay = null,
            lifetime = LifetimeTotals(28_000, 0.0, 0.0, 7),
            dailyGoal = 10_000
        )
        assertEquals((6L downTo 0L).map { today.minusDays(it).toString() }, stats.last7Days.map { it.date })
    }
}
