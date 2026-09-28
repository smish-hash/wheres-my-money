package com.smish.wheresmymoney.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {
    private val zone: ZoneId = ZoneId.systemDefault()

    /** [start, end) epoch-millis range covering the given month. */
    fun monthRange(yearMonth: YearMonth): Pair<Long, Long> {
        val start = yearMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    fun today(): Long = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

    fun formatMonth(yearMonth: YearMonth): String =
        "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${yearMonth.year}"

    fun formatDate(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
}
