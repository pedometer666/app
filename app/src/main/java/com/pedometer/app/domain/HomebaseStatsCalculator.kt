package com.pedometer.app.domain

import com.pedometer.app.data.local.BestDayRecord
import com.pedometer.app.data.local.BestDistanceRecord
import com.pedometer.app.data.local.LifetimeTotals
import com.pedometer.app.data.local.MonthlyTotals
import com.pedometer.app.data.local.StepEntity
import com.pedometer.app.util.DateUtils
import java.time.LocalDate

/**
 * Pure analytics for Homebase — no I/O, fully unit-testable.
 * Gaps in the recorded history are treated as zero-step days, which matches
 * how a pedometer should behave when the tracker was off.
 */
object HomebaseStatsCalculator {

    /** How far back streak detection looks. */
    const val STREAK_WINDOW_DAYS: Long = 370

    fun build(
        today: LocalDate,
        trailing14Days: List<StepEntity>,
        streakWindow: List<StepEntity>,
        monthToDate: MonthlyTotals,
        previousMonthToDate: MonthlyTotals,
        bestStepDay: BestDayRecord?,
        bestDistanceDay: BestDistanceRecord?,
        lifetime: LifetimeTotals,
        dailyGoal: Int
    ): HomebaseStats {
        val byDate = trailing14Days.associateBy { it.date }

        val last7Days = (6 downTo 0).map { offset ->
            val day = today.minusDays(offset.toLong())
            val key = DateUtils.dateKey(day)
            DayStepValue(date = key, steps = byDate[key]?.steps ?: 0)
        }
        val last7Sum = last7Days.sumOf { it.steps }.toDouble()

        val previous7Sum = (13 downTo 7).sumOf { offset ->
            byDate[DateUtils.dateKey(today.minusDays(offset.toLong()))]?.steps ?: 0
        }.toDouble()

        return HomebaseStats(
            weeklyAverageSteps = last7Sum / 7.0,
            previousWeekAverageSteps = previous7Sum / 7.0,
            weekOverWeekPercent = percentDelta(last7Sum, previous7Sum),
            last7Days = last7Days,
            monthToDateSteps = monthToDate.totalSteps,
            monthToDateDistanceMeters = monthToDate.totalDistanceMeters,
            monthToDateCalories = monthToDate.totalCalories,
            previousMonthToDateSteps = previousMonthToDate.totalSteps,
            previousMonthToDateDistanceMeters = previousMonthToDate.totalDistanceMeters,
            monthDeltaPercent = percentDeltaLong(monthToDate.totalSteps, previousMonthToDate.totalSteps),
            bestDaySteps = bestStepDay?.steps ?: 0,
            bestDayDate = bestStepDay?.date,
            bestDayDistanceMeters = bestDistanceDay?.distanceMeters ?: 0.0,
            bestDayDistanceDate = bestDistanceDay?.date,
            lifetimeSteps = lifetime.totalSteps,
            lifetimeDistanceMeters = lifetime.totalDistanceMeters,
            lifetimeActiveDays = lifetime.activeDays,
            currentStreakDays = currentStreak(today, streakWindow, dailyGoal)
        )
    }

    /**
     * Consecutive days meeting [dailyGoal]. Today does not break the streak —
     * it is still in progress, so if today is below goal we start counting
     * from yesterday instead.
     */
    fun currentStreak(
        today: LocalDate,
        window: List<StepEntity>,
        dailyGoal: Int
    ): Int {
        if (dailyGoal <= 0) return 0
        val byDate = window.associateBy { it.date }

        var day = today
        if ((byDate[DateUtils.dateKey(day)]?.steps ?: 0) < dailyGoal) {
            day = day.minusDays(1)
        }

        var streak = 0
        while (true) {
            val steps = byDate[DateUtils.dateKey(day)]?.steps ?: break
            if (steps >= dailyGoal) {
                streak++
                day = day.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    /** Signed percent delta in [-100, ∞). null when there is no meaningful baseline. */
    private fun percentDelta(current: Double, previous: Double): Float? {
        if (previous <= 0.0) return null
        return (((current - previous) / previous) * 100.0).toFloat()
    }

    private fun percentDeltaLong(current: Long, previous: Long): Float? =
        percentDelta(current.toDouble(), previous.toDouble())
}
