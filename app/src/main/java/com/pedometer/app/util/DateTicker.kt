package com.pedometer.app.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Emits the current [LocalDate] immediately, then re-emits at every midnight
 * (plus a small grace period) so date-keyed queries ("today", "this month")
 * roll over automatically while the app is open.
 */
fun midnightTicker(): Flow<LocalDate> = flow {
    while (true) {
        val now = LocalDateTime.now()
        emit(now.toLocalDate())
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay().plusSeconds(2)
        val millisUntilMidnight = Duration.between(now, nextMidnight).toMillis()
        delay(millisUntilMidnight.coerceAtLeast(1_000L))
    }
}
