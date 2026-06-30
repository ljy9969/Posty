package com.bimatrix.posty.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimatrix.posty.data.Task
import com.bimatrix.posty.ui.theme.Ink
import com.bimatrix.posty.ui.theme.InkSoft
import com.bimatrix.posty.ui.theme.stickyColor
import com.bimatrix.posty.ui.theme.stickyShade
import kotlin.math.PI
import kotlin.math.sin

// 라인 카드 기준 치수(StickyNoteCard 와 동일). 비행 노트의 기본 크기.
private val FlyW = StickyWidth      // 200.dp
private val FlyH = StickyHeight     // 250.dp

/** 라인 한 칸의 가로 보폭(카드 + 간격), 줌 적용 전. */
private val LineStrideBase = StickyWidth + StickySpacing  // 216.dp
private val LinePadStart = 24.dp                          // StickyRow contentPadding 과 동일

/** 덱 카드는 라인 카드보다 1.25배(=250/200) 크게 보인다. */
private const val DeckScale = 1.25f

/** 비행 한 번의 길이. 여유 있게 미끄러지듯. */
private const val FlightDurationMs = 820

/** 호(arc) 높이 — 책이 떠올랐다 내려앉는 정도(과하지 않게). */
private val ArcHeight = 70.dp

/** 진행 방향으로 살짝 기우는 정도(도). 흔들림 없이 비행 중 한 번만. */
private const val BankDeg = 9f

/** 책장 한 번 넘기듯 Y축으로 부드럽게 도는 정도(도). 모두 같은 방향. */
private const val FlipDeg = 16f

/** 한 화면에 최대 몇 장까지 날릴지(성능 상한). 나머지는 스크롤로 드러난다. */
private const val MaxFlying = 16

/** 튕김 없이 미끄러져 정착하는 이징(가속 → 부드러운 감속). */
private val GlideEasing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)

/**
 * 라인 영역: 깊이감 덱 ↔ 가로 펼침을 오간다.
 * 모드가 바뀌면 단순 크로스페이드 대신 "마법 걸린 책"처럼 카드들이 호를 그리며 펄럭이고,
 * 시차를 두고 덱 ↔ 라인 위치 사이를 날아가는 비행 애니메이션을 재생한다.
 *
 * - 펼칠 때(덱→라인): 앞(가까운) 장부터 차례로 스택에서 떠올라 제자리로 날아가 안착.
 * - 모일 때(라인→덱): 먼(뒤) 카드부터 차례로 날아와 중앙 스택으로 포개진다.
 */
@Composable
fun DeckLineBoard(
    tasks: List<Task>,
    deckMode: Boolean,
    zoom: Float,
    sortByDue: Boolean,
    onDeckChange: (Boolean) -> Unit,
    onTapTask: (Task) -> Unit,
    onCompleteTask: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 라인 표시 순서(마감순 정렬 시 임시 정렬). 비행도 이 순서를 따라 제자리에 안착한다.
    val lineTasks = if (sortByDue)
        tasks.sortedWith(compareBy(nullsLast()) { it.dueDate })
    else tasks

    // 라인의 스크롤 상태를 끌어올려 유지 → 비행이 실제 보이는 위치에서 출발/도착(점프 없음).
    val listState = rememberLazyListState()

    // 마감순/원래대로 버튼을 누르면(정렬 토글) 맨 앞 카드부터 보이도록 스크롤.
    LaunchedEffect(sortByDue) { listState.scrollToItem(0) }

    // 비행 상태.
    val progress = remember { Animatable(0f) }
    var animating by remember { mutableStateOf(false) }
    var spreading by remember { mutableStateOf(false) }   // true = 덱→라인(펼침)
    var primed by remember { mutableStateOf(false) }       // 첫 합성에서는 애니메이션 생략

    LaunchedEffect(deckMode) {
        if (!primed) { primed = true; return@LaunchedEffect }
        spreading = !deckMode          // deckMode=false → 라인으로 펼침
        animating = true
        progress.snapTo(0f)
        progress.animateTo(1f, tween(FlightDurationMs, easing = LinearEasing))
        animating = false
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }

        when {
            // 비행 중: 실제 덱/라인은 감추고 날아다니는 카드 오버레이만 그린다.
            animating -> {
                val stridePx = with(density) { (LineStrideBase * zoom).toPx() }
                val startPadPx = with(density) { LinePadStart.toPx() }
                // 라인에서 '지금 화면에 보이는' 카드들만 날린다(스크롤 위치 반영).
                // 첫 보이는 카드 인덱스/오프셋으로 각 칸의 라인 X 좌표를 해석적으로 복원.
                val firstIdx = listState.firstVisibleItemIndex.coerceIn(0, (lineTasks.size - 1).coerceAtLeast(0))
                val firstOffPx = listState.firstVisibleItemScrollOffset.toFloat()
                val flyingIdx = buildList {
                    var j = firstIdx
                    while (j < lineTasks.size && size < MaxFlying) {
                        val left = startPadPx + (j - firstIdx) * stridePx - firstOffPx
                        if (left >= wPx) break
                        add(j)
                        j++
                    }
                    if (isEmpty() && lineTasks.isNotEmpty()) add(0)
                }

                // 시차(stagger)·각 카드 기술자(描述子)를 한 번에 계산 → 카드와 반짝이가 같은 값을 공유.
                val n = flyingIdx.size
                val unit = if (n <= 1) 0f else (0.5f / (n - 1)).coerceAtMost(0.07f)
                val window = (1f - unit * (n - 1)).coerceAtLeast(0.2f)
                val cards = flyingIdx.mapIndexed { order, j ->
                    val task = lineTasks[j]
                    // 펼칠 땐 가까운(앞) 카드부터, 모일 땐 먼(뒤) 카드부터.
                    val delay = if (spreading) order * unit else (n - 1 - order) * unit
                    val jitterX = ((task.id.hashCode() % 5) - 2) * 2f
                    val lineLeftPx = startPadPx + (j - firstIdx) * stridePx - firstOffPx
                    FlyCard(task, order, delay, lineLeftPx, jitterX)
                }

                // 뒤(order 큰) 카드부터 그려, 맨 앞 카드(order 0)가 z축 최상단에 오게 한다
                // → 덱으로 모일 때 '맨 앞에 올 카드'가 제일 위에 보이며 마지막에 안착.
                Box(Modifier.fillMaxSize()) {
                    cards.asReversed().forEach { card ->
                        key(card.task.id) {
                            FlyingNote(
                                card = card,
                                window = window,
                                spreading = spreading,
                                progress = progress,
                                zoom = zoom,
                                wPx = wPx,
                                hPx = hPx,
                            )
                        }
                    }
                }
            }

            // 덱도 라인과 같은 순서(lineTasks) — 마감순이면 가장 급한 카드가 맨 앞(위)에 온다.
            deckMode -> DeckBoard(
                tasks = lineTasks,
                onTapTask = onTapTask,
                onCompleteTask = onCompleteTask,
                onTogglePin = onTogglePin,
                onSpread = { onDeckChange(false) },
                modifier = Modifier.fillMaxSize(),
            )

            else -> Box(
                Modifier
                    .fillMaxSize()
                    // 빈 영역 더블탭 → 다시 덱(스택)으로.
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { onDeckChange(true) })
                    },
            ) {
                StickyRow(
                    tasks = lineTasks,
                    zoom = zoom,
                    reorderable = !sortByDue,
                    onTapTask = onTapTask,
                    onCompleteTask = onCompleteTask,
                    onTogglePin = onTogglePin,
                    onReorder = onReorder,
                    listState = listState,
                )
                Text(
                    text = "빈 곳을 두 번 톡 하면 덱으로 모여요",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSoft,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp),
                )
            }
        }
    }
}

/** 비행 카드 한 장의 변하지 않는 정보(위치 계산용). 카드와 반짝이가 공유한다. */
private class FlyCard(
    val task: Task,
    val order: Int,
    val delay: Float,
    val lineLeftPx: Float,
    val jitterX: Float,
)

/** 진행도 t 에서의 카드 좌상단(px)·스케일·관련 값. graphicsLayer 와 반짝이 Canvas 가 공유. */
private class FlyState(
    val x: Float,
    val y: Float,
    val scale: Float,
    val e: Float,
    val ct: Float,
    val dir: Float,
)

/**
 * 카드 한 장의 현재 비행 상태를 [t] 로부터 계산. [Density] 확장이라
 * graphicsLayer(레이아웃 단계)·DrawScope(그리기 단계) 양쪽에서 같은 식으로 부른다.
 */
private fun Density.flyState(
    spreading: Boolean,
    order: Int,
    delay: Float,
    window: Float,
    t: Float,
    zoom: Float,
    wPx: Float,
    hPx: Float,
    lineLeftPx: Float,
    jitterX: Float,
): FlyState {
    val ct = ((t - delay) / window).coerceIn(0f, 1f)
    val e = GlideEasing.transform(ct)
    val baseW = FlyW.toPx()
    val baseH = FlyH.toPx()
    // 덱(중앙 스택) — 살짝 위로 포개지는 스택 느낌.
    val stackLift = -(if (order < 5) order else 5) * 4f
    val deckLeft = wPx / 2f - baseW * DeckScale / 2f + jitterX.dp.toPx()
    val deckTop = hPx / 2f - baseH * DeckScale / 2f + stackLift.dp.toPx()
    val lineTop = (hPx - baseH * zoom) / 2f
    val sx = if (spreading) deckLeft else lineLeftPx
    val ex = if (spreading) lineLeftPx else deckLeft
    val sy = if (spreading) deckTop else lineTop
    val ey = if (spreading) lineTop else deckTop
    val ss = if (spreading) DeckScale else zoom
    val es = if (spreading) zoom else DeckScale
    val x = sx + (ex - sx) * e
    val arc = -ArcHeight.toPx() * sin(e * PI).toFloat()   // 떠올랐다 부드럽게 내려앉음
    val y = sy + (ey - sy) * e + arc
    val sc = ss + (es - ss) * e
    val dir = if (ex >= sx) 1f else -1f
    return FlyState(x, y, sc, e, ct, dir)
}

/**
 * 날아다니는 카드 한 장. 모든 프레임 계산은 [graphicsLayer] 람다 안에서 [progress] 를 읽어
 * 레이아웃 단계(재합성 X)에서만 갱신되게 한다 → 많은 카드도 가볍게.
 */
@Composable
private fun FlyingNote(
    card: FlyCard,
    window: Float,
    spreading: Boolean,
    progress: Animatable<Float, *>,
    zoom: Float,
    wPx: Float,
    hPx: Float,
) {
    // 카드별 고정 회전값(StickyNoteCard 와 같은 규칙으로 라인 안착 시 자연스러운 기울기).
    val lineRot = remember(card.task.id) { ((card.task.id.hashCode() % 7) - 3).toFloat() }
    val deckRot = remember(card.task.id) { ((card.task.id.hashCode() % 3) - 1).toFloat() }

    Box(
        Modifier
            .size(FlyW, FlyH)
            .graphicsLayer {
                val s = flyState(
                    spreading, card.order, card.delay, window, progress.value,
                    zoom, wPx, hPx, card.lineLeftPx, card.jitterX,
                )
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = s.x
                translationY = s.y
                scaleX = s.scale
                scaleY = s.scale
                // 비행 중 정점에서만 한 번 부드럽게 — 흔들림(진동) 없음.
                val swing = sin(s.e * PI).toFloat()              // 0 → 1 → 0
                val sr = if (spreading) deckRot else lineRot
                val er = if (spreading) lineRot else deckRot
                rotationZ = (sr + (er - sr) * s.e) + s.dir * BankDeg * swing  // 진행 방향으로 살짝 기울었다 폄
                rotationY = FlipDeg * swing                    // 책장 한 번 넘기듯(모두 같은 방향)
                cameraDistance = 16f * density
            },
    ) {
        FlyingNoteVisual(card.task)
    }
}

/** 비행 중 보이는 정적인 포스트잇 모양(StickyNoteCard 의 외형만 간추림). */
@Composable
private fun FlyingNoteVisual(task: Task) {
    val paper = stickyColor(task.colorIndex)
    val shade = stickyShade(task.colorIndex)

    Box(
        Modifier
            .padding(top = 10.dp)
            .size(FlyW, FlyH)
            .shadow(8.dp, RoundedCornerShape(10.dp), clip = false)
            .clip(RoundedCornerShape(10.dp))
            .background(paper),
    ) {
        // 접힌 우하단 모서리
        Canvas(Modifier.fillMaxSize()) {
            val fold = 26.dp.toPx()
            val path = Path().apply {
                moveTo(size.width - fold, size.height)
                lineTo(size.width, size.height)
                lineTo(size.width, size.height - fold)
                close()
            }
            drawPath(path, shade)
        }
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // 비행 중에는 자동 크기조절(AutoResizeText) 대신 고정 크기 텍스트로 —
                // 폰트 맞춤 반복 측정/재합성이 없어 전환 시작의 버벅임을 없앤다.
                Text(
                    text = task.text,
                    color = Ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 25.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (task.pinned) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = null,
                tint = Color(0xFFE5604D),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(26.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.PushPin,
                contentDescription = null,
                tint = Ink.copy(alpha = 0.35f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(26.dp)
                    .graphicsLayer { rotationZ = 28f },
            )
        }
    }
}
