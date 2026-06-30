package com.bimatrix.posty.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bimatrix.posty.data.Task
import com.bimatrix.posty.ui.theme.Ink
import com.bimatrix.posty.ui.theme.InkSoft
import com.bimatrix.posty.ui.theme.Lavender
import com.bimatrix.posty.ui.theme.LavenderSoft
import com.bimatrix.posty.ui.theme.Mint
import com.bimatrix.posty.ui.theme.MintDark
import kotlin.math.roundToInt

/** 라인 모드 드래그 중 가장자리 자동 스크롤이 시작되는 영역 폭. */
private val AUTO_SCROLL_EDGE = 84.dp

/** 가장자리 끝에서의 최대 스크롤 속도(초당 dp) — '천천히'. */
private val AUTO_SCROLL_SPEED_MAX = 520.dp

/** 라인 모드 줌 한계(라인은 축소만). */
private const val LINE_MIN_ZOOM = 0.45f
private const val LINE_MAX_ZOOM = 1f

/** 좌하단 줌/우하단 + 버튼 공통 여백 — 라인·자유 화면 위치 통일. */
internal val CONTROL_EDGE = 20.dp

/** 하단 컨트롤(줌 · 마감순 · 추가) 공통 높이 — 한 줄로 어울리게 통일. */
internal val CONTROL_HEIGHT = 44.dp

/**
 * 메인 보드: 미완료 할 일을 우선순위 순으로 좌→우 가로 나열.
 * 카드를 길게 눌러 드래그하면 우선순위(좌우 위치)를 바꾼다.
 */
@Composable
fun BoardScreen(
    tasks: List<Task>,
    completedCount: Int,
    freeMode: Boolean,
    deckMode: Boolean,
    onDeckChange: (Boolean) -> Unit,
    canUndo: Boolean,
    lineZoom: Float,
    onLineZoomChange: (Float) -> Unit,
    freeZoom: Float,
    onFreeZoomChange: (Float) -> Unit,
    onTapTask: (Task) -> Unit,
    onCompleteTask: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onMoveNote: (String, Float, Float) -> Unit,
    onMoveNotes: (List<Triple<String, Float, Float>>) -> Unit,
    onUndo: () -> Unit,
    onUngroup: (List<Triple<String, Float, Float>>) -> Unit,
    onToggleMode: () -> Unit,
    onAdd: () -> Unit,
    onOpenCompleted: () -> Unit,
) {
    // 마감순 정렬 토글(라인 펼침 모드 전용, 표시용 임시 정렬). 끄면 사용자 배치 순서로 복원.
    var sortByDue by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BoardHeader(
                activeCount = tasks.size,
                completedCount = completedCount,
                freeMode = freeMode,
                onToggleMode = onToggleMode,
                onOpenCompleted = onOpenCompleted,
            )
            when {
                freeMode -> FreeBoard(
                    tasks = tasks,
                    canUndo = canUndo,
                    zoom = freeZoom,
                    onZoomChange = onFreeZoomChange,
                    onTapTask = onTapTask,
                    onCompleteTask = onCompleteTask,
                    onTogglePin = onTogglePin,
                    onMoveNote = onMoveNote,
                    onMoveNotes = onMoveNotes,
                    onUndo = onUndo,
                    onUngroup = onUngroup,
                    onAdd = onAdd,
                    modifier = Modifier.weight(1f),
                )

                tasks.isEmpty() -> EmptyBoard(Modifier.weight(1f))

                // 라인 모드: 덱(스택) ↔ 가로 펼침. 모드 전환은 "마법 걸린 책"처럼
                // 카드들이 날아가/모이는 비행 애니메이션으로 처리(DeckLineBoard).
                // 마감순 정렬은 표시용 임시 정렬이며, 정렬 중엔 재배치를 막아 사용자 순서를 보존.
                else -> DeckLineBoard(
                    tasks = tasks,
                    deckMode = deckMode,
                    zoom = lineZoom,
                    sortByDue = sortByDue,
                    onDeckChange = onDeckChange,
                    onTapTask = onTapTask,
                    onCompleteTask = onCompleteTask,
                    onTogglePin = onTogglePin,
                    onReorder = onReorder,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // 하단 컨트롤. 라인 펼침 모드(카드 2장 이상)에서는 줌·원래대로·추가를 한 줄에
        // 균등 간격(SpaceBetween)으로 배치 → 가운데 칩 양옆 간격이 같아진다.
        // 그 외(덱 모드·카드 1장 이하)에는 추가만 우하단. (자유 모드는 +를 다른 위치에 둠)
        if (!freeMode) {
            if (!deckMode && tasks.size > 1) {
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = CONTROL_EDGE, end = CONTROL_EDGE, bottom = CONTROL_EDGE),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ZoomControl(
                        zoom = lineZoom,
                        onZoomIn = { onLineZoomChange((lineZoom + 0.15f).coerceAtMost(LINE_MAX_ZOOM)) },
                        onZoomOut = { onLineZoomChange((lineZoom - 0.15f).coerceAtLeast(LINE_MIN_ZOOM)) },
                    )
                    DueSortButton(
                        active = sortByDue,
                        onClick = { sortByDue = !sortByDue },
                    )
                    AddPillButton(onClick = onAdd)
                }
            } else {
                AddPillButton(
                    onClick = onAdd,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = CONTROL_EDGE, bottom = CONTROL_EDGE),
                )
            }
        }
    }
}

@Composable
private fun BoardHeader(
    activeCount: Int,
    completedCount: Int,
    freeMode: Boolean,
    onToggleMode: () -> Unit,
    onOpenCompleted: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("오늘의 보드", style = MaterialTheme.typography.headlineSmall, color = Ink)
            Text(
                if (activeCount == 0) "할 일이 없어요. 한숨 돌리세요 :)" else "남은 할 일 $activeCount 장",
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
            )
        }
        // 보드 모드 전환 (라인 ↔ 자유) — 민트 칩
        Text(
            text = if (freeMode) "자유" else "라인",
            color = MintDark,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(CircleShape)
                .background(Mint.copy(alpha = 0.18f))
                .clickableNoRipple(onToggleMode)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Spacer(Modifier.width(8.dp))
        // 완료 보관함 — 라벤더 칩(모드 칩과 색 구분)
        Row(
            Modifier
                .clip(CircleShape)
                .background(LavenderSoft)
                .clickableNoRipple(onOpenCompleted)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.CheckCircle, null, tint = Lavender, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("완료 $completedCount", color = Lavender, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyBoard(modifier: Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📝", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "+ 버튼으로 할 일을 한 장씩 붙여보세요.\n작게 쪼갤수록 가벼워져요.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun StickyRow(
    tasks: List<Task>,
    zoom: Float,
    onTapTask: (Task) -> Unit,
    onCompleteTask: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    reorderable: Boolean = true,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    // 슬롯 폭은 카드 footprint(축소됨) + 간격(축소됨) → 카드 로컬 좌표계로 환산하면 항상 원본 stride.
    val strokePx = with(density) { (StickyWidth + StickySpacing).toPx() }

    // 드래그 중 실시간 재배치를 위한 로컬 사본.
    val local = remember(tasks) { tasks.toMutableStateList() }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragAmount by remember { mutableFloatStateOf(0f) }
    // 화면(뷰포트) 기준 손가락 X 좌표 — 가장자리 자동 스크롤 판단용.
    var pointerX by remember { mutableFloatStateOf(0f) }

    // 누적 이동량(로컬 px)을 받아 슬롯 단위로 재배치. 드래그/자동스크롤 양쪽에서 호출.
    val step by rememberUpdatedState<(Float) -> Unit> { deltaLocal ->
        dragAmount += deltaLocal
        val from = local.indexOfFirst { it.id == draggingId }
        if (from >= 0) {
            val pinnedCount = local.count { it.pinned }
            val pinned = local[from].pinned
            val lo = if (pinned) 0 else pinnedCount
            val hi = if (pinned) (pinnedCount - 1).coerceAtLeast(0) else local.size - 1
            val target = (from + (dragAmount / strokePx).roundToInt()).coerceIn(lo, hi)
            if (target != from) {
                local.add(target, local.removeAt(from))
                dragAmount -= (target - from) * strokePx
            }
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val viewportW = with(density) { maxWidth.toPx() }
        val edgePx = with(density) { AUTO_SCROLL_EDGE.toPx() }
        val speedPxPerSec = with(density) { AUTO_SCROLL_SPEED_MAX.toPx() }

        // 드래그 중 손가락이 가장자리에 머물면 천천히(가장자리에 가까울수록 조금 빠르게) 스크롤.
        // 단일 scroll 세션 + 프레임 시간 기반 → 프레임률과 무관하게 일정한 속도.
        LaunchedEffect(draggingId) {
            if (draggingId == null) return@LaunchedEffect
            listState.scroll {
                var last = 0L
                while (draggingId != null) {
                    val frame = withFrameNanos { it }
                    val dtSec = if (last == 0L) 0f else (frame - last) / 1_000_000_000f
                    last = frame
                    val id = draggingId ?: break
                    val from = local.indexOfFirst { it.id == id }
                    if (from < 0) continue
                    val pinnedCount = local.count { it.pinned }
                    val pinned = local[from].pinned
                    val lo = if (pinned) 0 else pinnedCount
                    val hi = if (pinned) (pinnedCount - 1).coerceAtLeast(0) else local.size - 1
                    // 가장자리 침투 깊이(0~1)로 속도를 정함.
                    val leftDepth = ((edgePx - pointerX) / edgePx).coerceIn(0f, 1f)
                    val rightDepth = ((pointerX - (viewportW - edgePx)) / edgePx).coerceIn(0f, 1f)
                    val dir = when {
                        leftDepth > 0f && from > lo && listState.canScrollBackward -> -1f
                        rightDepth > 0f && from < hi && listState.canScrollForward -> 1f
                        else -> 0f
                    }
                    if (dir != 0f && dtSec > 0f) {
                        val depth = if (dir < 0f) leftDepth else rightDepth
                        val moved = scrollBy(dir * speedPxPerSec * depth * dtSec)
                        if (moved != 0f) step(moved / zoom)
                    }
                }
            }
        }

        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                // 자식 드래그를 가로채지 않고(Initial 패스, 소비 안 함) 손가락 X만 관찰.
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val e = awaitPointerEvent(PointerEventPass.Initial)
                            e.changes.firstOrNull()?.let { pointerX = it.position.x }
                        }
                    }
                },
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(StickySpacing * zoom),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(local, key = { _, t -> t.id }) { _, task ->
                val isDragging = task.id == draggingId
                StickyNoteCard(
                    task = task,
                    isDragging = isDragging,
                    onTap = { onTapTask(task) },
                    onComplete = { onCompleteTask(task.id) },
                    onTogglePin = { onTogglePin(task.id) },
                    modifier = Modifier
                        .scaleLayout(zoom)
                        .pointerInput(local.size, reorderable) {
                            if (!reorderable) return@pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingId = task.id
                                    dragAmount = 0f
                                },
                                onDragEnd = {
                                    draggingId = null
                                    dragAmount = 0f
                                    onReorder(local.map { it.id })
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragAmount = 0f
                                    onReorder(local.map { it.id })
                                },
                                onDrag = { change, drag ->
                                    change.consume()
                                    step(drag.x)
                                },
                            )
                        },
                )
            }
        }
    }
}

/** 할 일 추가 버튼 — 줌·마감순과 같은 높이/라운드의 알약(민트 채움, 주 동작). */
@Composable
private fun AddPillButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(CONTROL_HEIGHT)
            .clip(CircleShape)
            .background(Mint)
            .clickableNoRipple(onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Add, "할 일 추가", tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            "추가",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * 마감순 정렬 토글 버튼 — 켜면 마감 빠른 순으로 임시 정렬, 다시 누르면 사용자 배치 순서로 복원.
 * 줌·추가 버튼과 같은 높이/라운드의 알약. 평소엔 흰 유틸 알약, 켜지면 민트로 채워 활성 표시.
 */
@Composable
private fun DueSortButton(active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // 글자는 검정(켜졌을 땐 민트 배경 위 흰색), 아이콘은 민트 포인트.
    val iconTint = if (active) Color.White else MintDark
    val textColor = if (active) Color.White else Ink
    Row(
        modifier
            .height(CONTROL_HEIGHT)
            .clip(CircleShape)
            .background(if (active) Mint else Color.White.copy(alpha = 0.92f))
            .clickableNoRipple(onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Schedule, null, tint = iconTint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            if (active) "원래대로" else "마감순",
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun ZoomControl(
    zoom: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .height(CONTROL_HEIGHT)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.92f))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).clickableNoRipple(onZoomOut),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Remove, "축소", tint = MintDark, modifier = Modifier.size(20.dp)) }
        Text("${(zoom * 100).roundToInt()}%", color = InkSoft, style = MaterialTheme.typography.labelSmall)
        Box(
            Modifier.size(36.dp).clip(CircleShape).clickableNoRipple(onZoomIn),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, "확대", tint = MintDark, modifier = Modifier.size(20.dp)) }
    }
}

/** 자식을 [scale] 배로 그리되, 레이아웃 footprint 도 같은 비율로 축소 보고 → 더 많은 카드가 보임. */
private fun Modifier.scaleLayout(scale: Float): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val w = (placeable.width * scale).roundToInt()
    val h = (placeable.height * scale).roundToInt()
    layout(w, h) {
        placeable.placeWithLayer(0, 0) {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0f, 0f)
        }
    }
}

/** 리플 없는 가벼운 클릭. onClick 이 매 컴포지션마다 바뀌어도(예: zoom 캡처) 항상 최신을 호출. */
internal fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    val latest by rememberUpdatedState(onClick)
    Modifier.pointerInput(Unit) { detectTapGestures(onTap = { latest() }) }
}
