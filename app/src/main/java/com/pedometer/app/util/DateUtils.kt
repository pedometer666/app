package com.pedometer.app.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Centralised date handling.
 *
 * Daily records are keyed by `yyyy-MM-dd` ([DateTimeFormatter.ISO_LOCAL_DATE]).
 * ISO-8601 dates order lexicographically, so Room string comparisons
 * (`date BETWEEN :start AND :end`) double as chronological range queries.
 */
object DateUtils {

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val monthFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val longDayFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())

    fun dateKey(date: LocalDate): String = date.format(dateFormatter)

    fun todayKey(): String = dateKey(LocalDate.now())

    fun monthStartKey(date: LocalDate = LocalDate.now()): String =
        dateKey(date.withDayOfMonth(1))

    fun monthEndKey(date: LocalDate = LocalDate.now()): String =
        dateKey(date.withDayOfMonth(date.lengthOfMonth()))

    /** e.g. "November 2026" */
    fun monthLabel(date: LocalDate = LocalDate.now()): String = date.format(monthFormatter)

    /** e.g. "Thursday, November 5" */
    fun todayLongLabel(): String = LocalDate.now().format(longDayFormatter)
}
