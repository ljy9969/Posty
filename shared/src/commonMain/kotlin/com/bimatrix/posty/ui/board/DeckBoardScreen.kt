package com.bimatrix.posty.ui.board

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimatrix.posty.data.Task
import com.bimatrix.posty.ui.components.AutoResizeText
import com.bimatrix.posty.ui.daysUntilDue
import com.bimatrix.posty.ui.dueLabel
import com.bimatrix.posty.ui.formatShortDow
import com.bimatrix.posty.ui.theme.Ink
import com.bimatrix.posty.ui.theme.InkSoft
import com.bimatrix.posty.ui.theme.MintDark
import com.bimatrix.posty.ui.theme.stickyColor
import com.bimatrix.posty.ui.theme.stickyShade

private val DeckCardW = 250.dp
private val DeckCardH = 316.dp

/** 앞 카드 뒤로 몇 장까지 입체로 보여줄지. */
private const val DECK_DEPTH = 3

private val PinRedDeck = Color(0xFFE5604D)

/**
 * 깊이감 덱(스택) 뷰 — 가장 중요한 한 장(맨 앞)에 집중하고, 나머지는 뒤로 작게 물러나 흐려진다.
 * 카드 탭은 즉시 편집(지연 없음). 카드 밖 빈 곳을 두 번 톡 하면 가로 펼침으로 전환([onSpread]).
 * 카드를 완료하면 다음 장이 스프링으로 올라온다.
 */
@Composable
fun DeckBoard(
    tasks: List<Task>,
    onTapTask: (Task) -> Unit,
    onCompleteTask: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onSpread: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            // 카드 밖 빈 곳 더블탭 → 가로 펼침. (카드는 자체 탭을 소비하므로 편집은 지연 없이 즉시.)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onSpread() })
            },
        contentAlignment = Alignment.Center,
    ) {
        if (tasks.isEmpty()) return@Box
        val shown = (DECK_DEPTH + 1).coerceAtMost(tasks.size)
        // 뒤 카드부터 그려 앞 카드가 맨 위에 오게.
        for (i in (shown - 1) downTo 0) {
            val task = tasks[i]
            key(task.id) {
                DeckLayer(
                    task = task,
                    depth = i,
                    isFront = i == 0,
                    onTap = { onTapTask(task) },
                    onComplete = { onCompleteTask(task.id) },
                    onTogglePin = { onTogglePin(task.id) },
                )
            }
        }
        Text(
            text = "${tasks.size}장 · 빈 곳을 두 번 톡 하면 펼쳐져요",
            style = MaterialTheme.typography.labelMedium,
            color = InkSoft,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // 우하단 추가 버튼(높이 CONTROL_HEIGHT, 여백 CONTROL_EDGE) 위로 올려 겹침 방지.
                .padding(bottom = CONTROL_EDGE + CONTROL_HEIGHT + 12.dp),
        )
    }
}

@Composable
private fun DeckLayer(
    task: Task,
    depth: Int,
    isFront: Boolean,
    onTap: () -> Unit,
    onComplete: () -> Unit,
    onTogglePin: () -> Unit,
) {
    val springSpec = spring<Float>(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow)
    val scale by animateFloatAsState(1f - depth * 0.085f, springSpec, label = "scale")
    val yOff by animateFloatAsState(-depth * 26f, springSpec, label = "y")
    val alpha by animateFloatAsState(1f - depth * 0.22f, springSpec, label = "alpha")
    val tilt by animateFloatAsState(depth * 6f, springSpec, label = "tilt")

    Box(
        Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = yOff.dp.toPx()
                this.alpha = alpha
                rotationX = tilt
                cameraDistance = 14f * density
            }
            .then(
                // 앞 카드 탭 → 즉시 편집(더블탭 판별 지연 없음). 펼침은 빈 곳 더블탭으로.
                if (isFront) Modifier.pointerInput(task.id) {
                    detectTapGestures(onTap = { onTap() })
                } else Modifier,
            ),
    ) {
        DeckCardVisual(task, interactive = isFront, onComplete = onComplete, onTogglePin = onTogglePin)
    }
}

@Composable
private fun DeckCardVisual(
    task: Task,
    interactive: Boolean,
    onComplete: () -> Unit,
    onTogglePin: () -> Unit,
) {
    val paper = stickyColor(task.colorIndex)
    val shade = stickyShade(task.colorIndex)
    val baseRotation = remember(task.id) { ((task.id.hashCode() % 5) - 2).toFloat() }

    Box(
        Modifier
            .size(DeckCardW, DeckCardH)
            .graphicsLayer { rotationZ = if (interactive) 0f else baseRotation }
            .shadow(if (interactive) 16.dp else 8.dp, RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(paper),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val fold = 30.dp.toPx()
            val p = Path().apply {
                moveTo(size.width - fold, size.height)
                lineTo(size.width, size.height)
                lineTo(size.width, size.height - fold)
                close()
            }
            drawPath(p, shade)
        }
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            DeckDateRow(task)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AutoResizeText(
                    text = task.text,
                    color = Ink,
                    maxFontSize = 28.sp,
                    minFontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (interactive) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.6f))
                            .clickable { onComplete() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Check, "완료", tint = MintDark, modifier = Modifier.size(30.dp))
                    }
                }
            }
        }
        Icon(
            imageVector = if (task.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
            contentDescription = if (task.pinned) "고정 해제" else "고정",
            tint = if (task.pinned) PinRedDeck else Ink.copy(alpha = 0.32f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(28.dp)
                .graphicsLayer { rotationZ = if (task.pinned) 0f else 28f }
                .then(if (interactive) Modifier.clickable { onTogglePin() } else Modifier),
        )
    }
}

@Composable
private fun DeckDateRow(task: Task) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.EditNote, null, tint = Ink.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(3.dp))
        Text(
            formatShortDow(task.createdAt),
            style = MaterialTheme.typography.labelMedium,
            color = Ink.copy(alpha = 0.6f),
        )
        if (task.dueDate != null) {
            Spacer(Modifier.weight(1f))
            val days = daysUntilDue(task.dueDate)
            val dueColor = when {
                days == null -> Ink
                days < 0 -> PinRedDeck
                days <= 1 -> MintDark
                else -> Ink.copy(alpha = 0.6f)
            }
            Icon(Icons.Rounded.Schedule, null, tint = dueColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(3.dp))
            Text(
                dueLabel(task.dueDate) ?: "",
                style = MaterialTheme.typography.labelMedium,
                color = dueColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
