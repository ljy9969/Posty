@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.bimatrix.posty.ui

import com.bimatrix.posty.platform.currentTimeMillis
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

private val KOR_DOW = arrayOf("월", "화", "수", "목", "금", "토", "일")

/** "6/25 (목)" 형태의 짧은 날짜 + 요일. */
fun formatShortDow(millis: Long): String {
    val d = toLocalDate(millis)
    return "${d.monthNumber}/${d.dayOfMonth} (${KOR_DOW[d.dayOfWeek.isoDayNumber - 1]})"
}

/** "2026. 6. 25." 형태의 전체 날짜. */
fun formatFull(millis: Long): String {
    val d = toLocalDate(millis)
    return "${d.year}. ${d.monthNumber}. ${d.dayOfMonth}."
}

/** 마감까지 남은 일수. 음수면 지났음, 0이면 오늘. null 이면 마감 없음. */
fun daysUntilDue(dueMillis: Long?): Long? {
    if (dueMillis == null) return null
    val today = toLocalDate(currentTimeMillis())
    return today.daysUntil(toLocalDate(dueMillis)).toLong()
}

/** 마감 D-day 라벨. ("오늘", "내일", "D-3", "3일 지남") */
fun dueLabel(dueMillis: Long?): String? {
    val days = daysUntilDue(dueMillis) ?: return null
    return when {
        days == 0L -> "오늘 마감"
        days == 1L -> "내일 마감"
        days > 1L -> "D-$days"
        days == -1L -> "어제 지남"
        else -> "${-days}일 지남"
    }
}
