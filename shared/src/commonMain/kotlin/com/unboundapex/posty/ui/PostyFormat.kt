@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.unboundapex.posty.ui

import com.unboundapex.posty.platform.currentTimeMillis
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime

private fun toLocalDate(millis: Long): LocalDate =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date

/** "6/25" 형태의 짧은 날짜. */
fun formatShort(millis: Long): String {
    val d = toLocalDate(millis)
    return "${d.monthNumber}/${d.dayOfMonth}"
}

private val DOW = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** "6/25 (Thu)" 형태의 짧은 날짜 + 요일. */
fun formatShortDow(millis: Long): String {
    val d = toLocalDate(millis)
    return "${d.monthNumber}/${d.dayOfMonth} (${DOW[d.dayOfWeek.isoDayNumber - 1]})"
}

/** "Jun 25, 2026" 형태의 전체 날짜. */
fun formatFull(millis: Long): String {
    val d = toLocalDate(millis)
    return "${MONTHS[d.monthNumber - 1]} ${d.dayOfMonth}, ${d.year}"
}

/** 마감까지 남은 일수. 음수면 지났음, 0이면 오늘. null 이면 마감 없음. */
fun daysUntilDue(dueMillis: Long?): Long? {
    if (dueMillis == null) return null
    val today = toLocalDate(currentTimeMillis())
    return today.daysUntil(toLocalDate(dueMillis)).toLong()
}

/** 마감 D-day 라벨. ("Due today", "Due tomorrow", "D-3", "3 days overdue") */
fun dueLabel(dueMillis: Long?): String? {
    val days = daysUntilDue(dueMillis) ?: return null
    return when {
        days == 0L -> "Due today"
        days == 1L -> "Due tomorrow"
        days > 1L -> "D-$days"
        days == -1L -> "1 day overdue"
        else -> "${-days} days overdue"
    }
}
