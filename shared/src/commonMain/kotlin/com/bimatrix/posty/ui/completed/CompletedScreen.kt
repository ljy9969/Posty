@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.bimatrix.posty.ui.completed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimatrix.posty.data.Task
import com.bimatrix.posty.ui.formatShortDow
import com.bimatrix.posty.ui.theme.Ink
import com.bimatrix.posty.ui.theme.InkSoft
import com.bimatrix.posty.ui.theme.Mint
import com.bimatrix.posty.ui.theme.MintDark
import com.bimatrix.posty.ui.theme.MintSoft
import com.bimatrix.posty.ui.theme.stickyColor
import com.bimatrix.posty.platform.currentTimeMillis
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.ceil

/** kotlinx-datetime 에는 YearMonth 가 없어 직접 둔다(연·월 + 달력 계산 헬퍼). */
private data class YM(val year: Int, val month: Int) {
    fun atDay(day: Int): LocalDate = LocalDate(year, month, day)
    fun plusMonths(n: Int): YM {
        val d = LocalDate(year, month, 1).plus(DatePeriod(months = n))
        return YM(d.year, d.monthNumber)
    }
    fun minusMonths(n: Int): YM = plusMonths(-n)
    val lengthOfMonth: Int
        get() = LocalDate(year, month, 1).daysUntil(LocalDate(year, month, 1).plus(DatePeriod(months = 1)))

    companion object {
        fun from(date: LocalDate): YM = YM(date.year, date.monthNumber)
    }
}

/** 요일 이름 — dayOfWeek.value(월=1…일=7) - 1 로 색인. */
private val DOW_NAME = arrayOf("월", "화", "수", "목", "금", "토", "일")

/** 달력 헤더 표시 순서 — 일요일 시작. */
private val WEEK_HEADERS = arrayOf("일", "월", "화", "수", "목", "금", "토")

private val SunRed = Color(0xFFE5604D)

/**
 * 완료 보관함을 '캘린더'로 — 완료한 날짜에 점이 찍히고, 날짜를 탭하면
 * 그날 클리어한 일들이 아래에 펼쳐진다(되돌리기 · 삭제 가능). 해낸 기록을 한눈에.
 */
@Composable
fun CompletedScreen(
    tasks: List<Task>,
    onBack: () -> Unit,
    onRestore: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val zone = remember { TimeZone.currentSystemDefault() }
    // 완료일(LocalDate) 기준으로 묶음.
    val byDate: Map<LocalDate, List<Task>> = remember(tasks) {
        tasks.filter { it.completedAt != null }
            .groupBy { Instant.fromEpochMilliseconds(it.completedAt!!).toLocalDateTime(zone).date }
    }
    val today = remember { Instant.fromEpochMilliseconds(currentTimeMillis()).toLocalDateTime(zone).date }
    val latestDate = remember(byDate) { byDate.keys.maxOrNull() }

    var month by remember { mutableStateOf(YM.from(latestDate ?: today)) }
    var selected by remember { mutableStateOf(latestDate) }

    Column(Modifier.fillMaxSize()) {
        // 헤더
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "뒤로", tint = Ink)
            }
            Column {
                Text("완료한 일", style = MaterialTheme.typography.headlineSmall, color = Ink)
                Text("지금까지 ${tasks.size}장 클리어 🎉", style = MaterialTheme.typography.labelSmall, color = InkSoft)
            }
        }

        if (tasks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "아직 완료한 일이 없어요.\n한 장씩 클리어해 보세요!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                    textAlign = TextAlign.Center,
                )
            }
            return
        }

        // 월 이동
        MonthHeader(
            month = month,
            monthCount = byDate.entries.filter { YM.from(it.key) == month }.sumOf { it.value.size },
            onPrev = {
                month = month.minusMonths(1)
                selected = byDate.keys.filter { YM.from(it) == month }.maxOrNull()
            },
            onNext = {
                month = month.plusMonths(1)
                selected = byDate.keys.filter { YM.from(it) == month }.maxOrNull()
            },
        )

        WeekdayRow()

        MonthGrid(
            month = month,
            byDate = byDate,
            selected = selected,
            today = today,
            onSelect = { selected = it },
        )

        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(1.dp)
                .background(InkSoft.copy(alpha = 0.18f)),
        )

        // 선택한 날짜의 완료 목록
        DayDetail(
            date = selected,
            dayTasks = selected?.let { byDate[it] }.orEmpty(),
            onRestore = onRestore,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun MonthHeader(
    month: YM,
    monthCount: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "이전 달", tint = Ink)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${month.year}년 ${month.month}월",
                style = MaterialTheme.typography.titleLarge,
                color = Ink,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (monthCount > 0) "이 달 $monthCount 장 클리어" else "이 달엔 아직 없어요",
                style = MaterialTheme.typography.labelSmall,
                color = if (monthCount > 0) MintDark else InkSoft,
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "다음 달", tint = Ink)
        }
    }
}

@Composable
private fun WeekdayRow() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        WEEK_HEADERS.forEachIndexed { i, d ->
            Text(
                d,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = if (i == 0) SunRed.copy(alpha = 0.8f) else InkSoft,
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YM,
    byDate: Map<LocalDate, List<Task>>,
    selected: LocalDate?,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
    val firstOfMonth = month.atDay(1)
    val offset = firstOfMonth.dayOfWeek.isoDayNumber % 7 // 일=0, 월=1 … 토=6
    val daysInMonth = month.lengthOfMonth
    val rows = ceil((offset + daysInMonth) / 7f).toInt()

    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val dayNum = r * 7 + c - offset + 1
                    if (dayNum in 1..daysInMonth) {
                        val date = month.atDay(dayNum)
                        DayCell(
                            date = date,
                            count = byDate[date]?.size ?: 0,
                            isSelected = date == selected,
                            isToday = date == today,
                            isSunday = c == 0,
                            onSelect = onSelect,
                        )
                    } else {
                        Box(Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(
    date: LocalDate,
    count: Int,
    isSelected: Boolean,
    isToday: Boolean,
    isSunday: Boolean,
    onSelect: (LocalDate) -> Unit,
) {
    val hasItems = count > 0
    val bg = when {
        isSelected -> Mint
        hasItems -> MintSoft
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> Color.White
        isSunday -> SunRed
        else -> Ink
    }
    Box(
        Modifier
            .weight(1f)
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(CircleShape)
            .background(bg)
            .then(
                if (isToday && !isSelected) Modifier.border(1.5.dp, Mint, CircleShape) else Modifier,
            )
            .clickable { onSelect(date) },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${date.dayOfMonth}",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (hasItems || isSelected) FontWeight.Bold else FontWeight.Normal,
            )
            // 완료가 있는 날: 점 (선택된 날은 흰 점)
            Box(
                Modifier
                    .padding(top = 2.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isSelected && hasItems -> Color.White
                            hasItems -> Mint
                            else -> Color.Transparent
                        },
                    ),
            )
        }
    }
}

@Composable
private fun DayDetail(
    date: LocalDate?,
    dayTasks: List<Task>,
    onRestore: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    if (date == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "날짜를 탭하면 그날 완료한 일이 보여요.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    val label = "${date.monthNumber}/${date.dayOfMonth} (${DOW_NAME[date.dayOfWeek.isoDayNumber - 1]})"

    if (dayTasks.isEmpty()) {
        Column(Modifier.fillMaxSize()) {
            Text(
                "$label · 0장",
                style = MaterialTheme.typography.titleSmall,
                color = Ink,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp),
            )
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "이 날 완료한 일이 없어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                )
            }
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            Text(
                "$label · ${dayTasks.size}장 클리어",
                style = MaterialTheme.typography.titleSmall,
                color = Ink,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
            )
        }
        items(dayTasks, key = { it.id }) { task ->
            CompletedRow(task, onRestore = { onRestore(task.id) }, onDelete = { onDelete(task.id) })
        }
    }
}

@Composable
private fun CompletedRow(task: Task, onRestore: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(stickyColor(task.colorIndex).copy(alpha = 0.5f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                task.text,
                style = MaterialTheme.typography.bodyLarge,
                color = Ink.copy(alpha = 0.7f),
                textDecoration = TextDecoration.LineThrough,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "작성 ${formatShortDow(task.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
                fontSize = 11.sp,
            )
        }
        IconButton(onClick = onRestore) {
            Icon(Icons.AutoMirrored.Rounded.Undo, "되돌리기", tint = MintDark, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.DeleteOutline, "삭제", tint = SunRed, modifier = Modifier.size(20.dp))
        }
    }
}
