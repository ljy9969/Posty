package com.bimatrix.posty.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimatrix.posty.data.Task
import com.bimatrix.posty.ui.components.AutoResizeText
import com.bimatrix.posty.ui.dueLabel
import com.bimatrix.posty.ui.formatShortDow
import com.bimatrix.posty.ui.theme.Ink
import com.bimatrix.posty.ui.theme.Mint
import com.bimatrix.posty.ui.theme.MintDark
import com.bimatrix.posty.ui.theme.stickyColor
import com.bimatrix.posty.ui.theme.stickyShade

private val FreeNoteW = 122.dp
private val FreeNoteH = 134.dp
private val PinRedFree = Color(0xFFE5604D)

private const val MIN_FREE_ZOOM = 0.35f
private const val MAX_FREE_ZOOM = 1f
private const val FREE_ZOOM_STEP = 0.15f

/**
 * 자유 배치 보드 — 포스트잇을 길게 눌러 어디로든 옮기고,
 * 다른 포스트잇과 겹치면 자동으로 한 묶음(스택)이 된다.
 * 스택은 카드처럼 쌓여 보이고, 우상단 칩으로 위에 올라올 카드를 바꾼다(플립).
 * 두 손가락(또는 좌하단 -/+)으로 확대·축소, 좌하단 되돌리기로 직전 배치 취소.
 */
@Composable
fun FreeBoard(
    tasks: List<Task>,
    canUndo: Boolean,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    onTapTask: (Task) -> Unit,
    onCompleteTask: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onMoveNote: (String, Float, Float) -> Unit,
    onMoveNotes: (List<Triple<String, Float, Float>>) -> Unit,
    onUndo: () -> Unit,
    onUngroup: (List<Triple<String, Float, Float>>) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val frontOf = remember { mutableStateMapOf<String, String>() }

    BoxWithConstraints(modifier.fillMaxSize()) {
        // 배치 가능한 '월드' 범위는 줌과 무관하게 고정(최소 줌에서 보이는 전체 영역).
        // → 축소 상태에서 넓게 배치해도 좌표가 줌에 따라 망가지지 않는다. 최소 줌으로 줄이면 전부 보임.
        val maxX = maxWidth / MIN_FREE_ZOOM - FreeNoteW
        val maxY = maxHeight / MIN_FREE_ZOOM - FreeNoteH
        // 미니맵용: 기본 화면 크기(줌 무관)와 현재 보이는 영역(줌에 따라 변함).
        val baseW = maxWidth.value
        val baseH = maxHeight.value
        val viewW = baseW / zoom
        val viewH = baseH / zoom

        // 위치 보정(미배치 노트는 느슨한 그리드로).
        val placed = tasks.mapIndexed { i, t ->
            val x = t.posX ?: (16f + (i % 3) * 124f)
            val y = t.posY ?: (16f + (i / 3) * 150f)
            t to androidx.compose.ui.geometry.Offset(x, y)
        }

        val groups = placed.groupBy { it.first.groupId }

        // 줌이 적용되는 캔버스 (확대·축소는 좌하단 -/+ 버튼으로).
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    transformOrigin = TransformOrigin(0f, 0f)
                },
        ) {
            // 단독 노트
            groups[null]?.forEach { (task, base) ->
                DraggableNote(
                    task = task,
                    baseX = base.x, baseY = base.y,
                    maxX = maxX, maxY = maxY,
                    density = density,
                    interactive = true,
                    onTap = { onTapTask(task) },
                    onComplete = { onCompleteTask(task.id) },
                    onTogglePin = { onTogglePin(task.id) },
                    onMoved = { x, y -> onMoveNote(task.id, x, y) },
                )
            }

            // 그룹(스택) — 통째로 함께 이동.
            groups.forEach { (gid, members) ->
                if (gid == null) return@forEach
                val sorted = members.sortedBy { it.first.id }
                val frontId = frontOf[gid]?.takeIf { id -> sorted.any { it.first.id == id } }
                    ?: sorted.first().first.id
                GroupStack(
                    members = sorted,
                    frontId = frontId,
                    maxX = maxX, maxY = maxY,
                    density = density,
                    onTap = { onTapTask(it) },
                    onComplete = { onCompleteTask(it) },
                    onTogglePin = { onTogglePin(it) },
                    onSelect = { id -> frontOf[gid] = id },
                    onFlip = {
                        val ids = sorted.map { it.first.id }
                        val cur = ids.indexOf(frontId)
                        frontOf[gid] = ids[(cur + 1) % ids.size]
                    },
                    onMoveMembers = onMoveNotes,
                    onUngroup = onUngroup,
                )
            }
        }

        if (tasks.isEmpty()) {
            Text(
                "+ 버튼으로 할 일을 붙이고,\n겹쳐 놓으면 한 묶음이 돼요.",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            // 좌하단: 줌 + 되돌리기 + 추가(+). 라인 화면과 같은 위치.
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = CONTROL_EDGE, end = CONTROL_EDGE, bottom = CONTROL_EDGE),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ZoomControl(
                    zoom = zoom,
                    onZoomIn = { onZoomChange((zoom + FREE_ZOOM_STEP).coerceAtMost(MAX_FREE_ZOOM)) },
                    onZoomOut = { onZoomChange((zoom - FREE_ZOOM_STEP).coerceAtLeast(MIN_FREE_ZOOM)) },
                )
                UndoButton(enabled = canUndo, onClick = onUndo)
                AddButton(onClick = onAdd)
            }

            // 우하단: 미니맵 — 화면 밖 카드 위치까지 한눈에.
            MiniMap(
                placed = placed,
                baseW = baseW,
                baseH = baseH,
                viewW = viewW,
                viewH = viewH,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = CONTROL_EDGE, bottom = CONTROL_EDGE),
            )
        }
    }
}

/** 좌하단 추가(+) 버튼 — 자유 화면에서 되돌리기 옆에 둔다. */
@Composable
private fun AddButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Mint)
            .clickableNoRipple(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Add, "할 일 추가", tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

/**
 * 미니맵 — 전체 카드 범위(보이는 영역 포함)를 작게 그려 화면 밖 카드 위치를 보여준다.
 * 반투명 사각형 = 현재 보이는 영역, 점 = 각 카드(색상 유지).
 */
@Composable
private fun MiniMap(
    placed: List<Pair<Task, androidx.compose.ui.geometry.Offset>>,
    baseW: Float,
    baseH: Float,
    viewW: Float,
    viewH: Float,
    modifier: Modifier,
) {
    if (placed.isEmpty()) return
    // 월드(미니맵 전체 범위) = 줌과 무관하게 '기본 화면 + 모든 카드 범위'로 고정.
    // → 줌이 바뀌어도 미니맵 모양·카드 위치는 그대로, 보이는 영역 사각형만 바뀐다.
    val worldW = (placed.maxOf { it.second.x } + FreeNoteW.value).coerceAtLeast(baseW).coerceAtLeast(1f)
    val worldH = (placed.maxOf { it.second.y } + FreeNoteH.value).coerceAtLeast(baseH).coerceAtLeast(1f)

    val mapW = 84.dp
    val aspect = (worldH / worldW).coerceIn(0.5f, 1.6f)
    val mapH = mapW * aspect

    Box(
        modifier
            .size(mapW, mapH)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.92f)),
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize().padding(4.dp)) {
            val sx = size.width / worldW
            val sy = size.height / worldH
            // 보이는 영역(좌상단 기준).
            drawRect(
                color = MintDark.copy(alpha = 0.14f),
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(
                    (viewW * sx).coerceAtMost(size.width),
                    (viewH * sy).coerceAtMost(size.height),
                ),
            )
            drawRect(
                color = MintDark.copy(alpha = 0.5f),
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(
                    (viewW * sx).coerceAtMost(size.width),
                    (viewH * sy).coerceAtMost(size.height),
                ),
                style = Stroke(width = 1.dp.toPx()),
            )
            // 카드 점.
            val dw = (FreeNoteW.value * sx).coerceAtLeast(3.dp.toPx())
            val dh = (FreeNoteH.value * sy).coerceAtLeast(3.dp.toPx())
            placed.forEach { (task, pos) ->
                drawRoundRect(
                    color = stickyColor(task.colorIndex),
                    topLeft = Offset(pos.x * sx, pos.y * sy),
                    size = androidx.compose.ui.geometry.Size(dw, dh),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun UndoButton(enabled: Boolean, onClick: () -> Unit) {
    val tint = if (enabled) MintDark else Ink.copy(alpha = 0.3f)
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.92f else 0.5f))
            .then(if (enabled) Modifier.clickableNoRipple(onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Undo, "되돌리기", tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun DraggableNote(
    task: Task,
    baseX: Float,
    baseY: Float,
    maxX: Dp,
    maxY: Dp,
    density: androidx.compose.ui.unit.Density,
    interactive: Boolean,
    onTap: () -> Unit,
    onComplete: () -> Unit,
    onTogglePin: () -> Unit,
    onMoved: (Float, Float) -> Unit,
) {
    val maxXf = (maxX.value).coerceAtLeast(0f)
    val maxYf = (maxY.value).coerceAtLeast(0f)
    // 저장된 실제 위치 그대로 표시(보정하지 않음). 화면 밖이면 축소하거나 미니맵으로 찾는다.
    var pos by remember(task.id, baseX, baseY) { mutableStateOf(baseX to baseY) }
    var dragging by remember { mutableStateOf(false) }

    Box(
        Modifier
            .offset(pos.first.dp, pos.second.dp)
            .graphicsLayer { if (dragging) { scaleX = 1.06f; scaleY = 1.06f } }
            .pointerInput(task.id) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(task.id) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false; onMoved(pos.first, pos.second) },
                    onDragCancel = { dragging = false; onMoved(pos.first, pos.second) },
                    onDrag = { change, drag ->
                        change.consume()
                        val nx = (pos.first + drag.x / density.density).coerceIn(0f, maxXf.coerceAtLeast(0f))
                        val ny = (pos.second + drag.y / density.density).coerceIn(0f, maxYf.coerceAtLeast(0f))
                        pos = nx to ny
                    },
                )
            },
    ) {
        NoteContent(task, interactive = interactive, onComplete = onComplete, onTogglePin = onTogglePin)
    }
}

/** 카드 사이 가로 펼침 간격(dp) — 묶인 카드들의 내용이 한눈에 보이도록 부채꼴로 펼친다. */
private const val FAN_X = 74f
private const val FAN_Y = 14f

/** 그룹 해제 시 카드들을 세로로 벌려 놓는 간격(dp) — 겹침 임계값(120)보다 커서 다시 안 묶임. */
private const val UNGROUP_GAP = 152f

/**
 * 겹쳐 묶인 카드 묶음. 부채꼴로 살짝 펼쳐 어떤 카드들이 묶였는지 보이게 한다.
 * 맨 오른쪽(앞) 카드만 완료/고정/편집·드래그 가능하고, 뒤 카드를 탭하면 앞으로 올라온다.
 * 앞 카드를 길게 끌면 묶음 전체가 함께 이동한다.
 */
@Composable
private fun GroupStack(
    members: List<Pair<Task, androidx.compose.ui.geometry.Offset>>,
    frontId: String,
    maxX: Dp,
    maxY: Dp,
    density: androidx.compose.ui.unit.Density,
    onTap: (Task) -> Unit,
    onComplete: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onSelect: (String) -> Unit,
    onFlip: () -> Unit,
    onMoveMembers: (List<Triple<String, Float, Float>>) -> Unit,
    onUngroup: (List<Triple<String, Float, Float>>) -> Unit,
) {
    val anchor = members.first().second
    val n = members.size
    val fanW = (n - 1) * FAN_X
    // 부채꼴(뒤로 갈수록 우하단으로 펼쳐짐)까지 화면 안에 들어오도록 앵커 가능 범위를 좁힌다.
    val maxXf = (maxX.value - fanW).coerceAtLeast(0f)
    val maxYf = (maxY.value - (n - 1) * FAN_Y).coerceAtLeast(0f)

    // 저장된 실제 위치 그대로 표시(보정하지 않음).
    var pos by remember(members.map { it.first.id }, anchor.x, anchor.y) {
        mutableStateOf(anchor.x to anchor.y)
    }
    var dragging by remember { mutableStateOf(false) }

    val ax = pos.first
    val ay = pos.second
    val front = members.first { it.first.id == frontId }
    val backs = members.filter { it.first.id != frontId }
    // 그리기 순서: 뒤 카드(왼쪽) → 앞 카드(오른쪽, 맨 위)
    val ordered = backs + listOf(front)

    fun commit() {
        dragging = false
        onMoveMembers(members.map { Triple(it.first.id, pos.first, pos.second) })
    }

    ordered.forEachIndexed { k, (task, _) ->
        val x = ax + k * FAN_X
        val y = ay + k * FAN_Y
        val isFront = task.id == frontId
        // 어느 카드를 길게 끌어도 묶음 전체가 함께 이동(작게 축소돼 있어도 잡기 쉽게).
        // 짧게 탭: 앞 카드=편집, 뒤 카드=앞으로.
        Box(
            Modifier
                .offset(x.dp, y.dp)
                .graphicsLayer {
                    if (dragging) { scaleX = 1.05f; scaleY = 1.05f }
                    if (!isFront) rotationZ = ((task.id.hashCode() % 5) - 2).toFloat()
                }
                .pointerInput(frontId, task.id) {
                    detectTapGestures(onTap = { if (isFront) onTap(task) else onSelect(task.id) })
                }
                .pointerInput(members.size, frontId, task.id) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = { commit() },
                        onDragCancel = { commit() },
                        onDrag = { change, d ->
                            change.consume()
                            val nx = (pos.first + d.x / density.density).coerceIn(0f, maxXf)
                            val ny = (pos.second + d.y / density.density).coerceIn(0f, maxYf)
                            pos = nx to ny
                        },
                    )
                },
        ) {
            NoteContent(
                task,
                interactive = isFront,
                onComplete = { onComplete(task.id) },
                onTogglePin = { onTogglePin(task.id) },
            )
        }
    }
    // 개수+플립 칩, 해제 칩 (묶음 좌상단 위)
    Row(
        Modifier.offset((ax + 2f).dp, (ay - 14f).dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StackChip(count = n, onFlip = onFlip)
        UngroupChip(onClick = {
            val baseX = pos.first.coerceIn(0f, maxX.value.coerceAtLeast(0f))
            val baseY = pos.second.coerceIn(0f, (maxY.value - (n - 1) * UNGROUP_GAP).coerceAtLeast(0f))
            val spread = members.mapIndexed { k, m ->
                Triple(m.first.id, baseX, (baseY + k * UNGROUP_GAP).coerceIn(0f, maxY.value.coerceAtLeast(0f)))
            }
            onUngroup(spread)
        })
    }
}

@Composable
private fun NoteContent(
    task: Task,
    interactive: Boolean,
    onComplete: () -> Unit,
    onTogglePin: () -> Unit,
) {
    val paper = stickyColor(task.colorIndex)
    val shade = stickyShade(task.colorIndex)
    Box(
        Modifier
            .size(FreeNoteW, FreeNoteH)
            .shadow(6.dp, RoundedCornerShape(9.dp), clip = false)
            .clip(RoundedCornerShape(9.dp))
            .background(paper),
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val fold = 18.dp.toPx()
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width - fold, size.height)
                lineTo(size.width, size.height)
                lineTo(size.width, size.height - fold)
                close()
            }
            drawPath(p, shade)
        }
        Column(Modifier.fillMaxSize().padding(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // 작성일 좌측 / 마감일 우측
                Text(formatShortDow(task.createdAt), style = MaterialTheme.typography.labelSmall, color = Ink.copy(alpha = 0.55f))
                task.dueDate?.let {
                    Spacer(Modifier.weight(1f))
                    Text(dueLabel(it) ?: "", style = MaterialTheme.typography.labelSmall, color = MintDark)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AutoResizeText(
                    text = task.text,
                    color = Ink,
                    maxFontSize = 16.sp,
                    minFontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (interactive) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.6f))
                            .clickable { onComplete() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Check, "완료", tint = MintDark, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        // 압정
        Icon(
            imageVector = if (task.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
            contentDescription = "고정",
            tint = if (task.pinned) PinRedFree else Ink.copy(alpha = 0.3f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(18.dp)
                .graphicsLayer { rotationZ = if (task.pinned) 0f else 28f }
                .then(if (interactive) Modifier.clickable { onTogglePin() } else Modifier),
        )
    }
}

@Composable
private fun StackChip(count: Int, onFlip: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MintDark)
            .clickable { onFlip() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text("${count}장", color = Color.White, fontSize = 11.sp)
        Icon(Icons.Rounded.SwapHoriz, "플립", tint = Color.White, modifier = Modifier.size(14.dp))
    }
}

/** 그룹 해제 칩 — 묶음을 풀어 카드들을 따로 떼어 놓는다. */
@Composable
private fun UngroupChip(onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(Icons.Rounded.CallSplit, "해제", tint = PinRedFree, modifier = Modifier.size(13.dp))
        Text("해제", color = PinRedFree, fontSize = 11.sp)
    }
}
