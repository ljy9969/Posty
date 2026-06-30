package com.bimatrix.posty.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimatrix.posty.data.Task
import com.bimatrix.posty.ui.components.AutoResizeText
import com.bimatrix.posty.ui.daysUntilDue
import com.bimatrix.posty.ui.dueLabel
import com.bimatrix.posty.ui.formatShortDow
import com.bimatrix.posty.ui.theme.Ink
import com.bimatrix.posty.ui.theme.Mint
import com.bimatrix.posty.ui.theme.MintDark
import com.bimatrix.posty.ui.theme.stickyColor
import com.bimatrix.posty.ui.theme.stickyShade

val StickyWidth = 200.dp
val StickyHeight = 250.dp
val StickySpacing = 16.dp

private val PinRed = Color(0xFFE5604D)

/**
 * 보드에 놓이는 포스트잇 한 장.
 * - 상단: 작성일 / 마감 D-day
 * - 중앙: 할 일 (자동 크기 조절)
 * - 하단: 완료 버튼 (누르면 체크 도장 + 줄긋기 애니메이션 후 [onComplete])
 * - 상단 중앙 압정: 탭하면 고정/해제 ([onTogglePin])
 * 드래그 제스처는 [modifier] 로 보드에서 주입한다.
 */
@Composable
fun StickyNoteCard(
    task: Task,
    onTap: () -> Unit,
    onComplete: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
) {
    val haptics = LocalHapticFeedback.current
    val baseRotation = remember(task.id) { ((task.id.hashCode() % 7) - 3).toFloat() }

    var completing by remember { mutableStateOf(false) }
    val stamp = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }
    val overshoot = remember { CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f) }

    LaunchedEffect(completing) {
        if (completing) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            stamp.animateTo(1f, tween(360, easing = overshoot))
            fade.animateTo(1f, tween(240))
            onComplete()
        }
    }

    val rotation by animateFloatAsState(if (isDragging) 0f else baseRotation, label = "rot")
    val scale by animateFloatAsState(if (isDragging) 1.06f else 1f, label = "scale")
    val elevation by animateDpAsState(if (isDragging) 18.dp else 8.dp, label = "elev")

    val paper = stickyColor(task.colorIndex)
    val shade = stickyShade(task.colorIndex)

    Box(
        modifier = modifier
            .width(StickyWidth)
            .height(StickyHeight)
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
                alpha = 1f - fade.value
                translationY = -fade.value * 60.dp.toPx()
            },
    ) {
        Box(
            Modifier
                .padding(top = 10.dp)
                .fillMaxSize()
                .shadow(elevation, RoundedCornerShape(10.dp), clip = false)
                .clip(RoundedCornerShape(10.dp))
                .background(paper)
                .clickable(enabled = !completing) { onTap() },
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
                DateRow(task)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AutoResizeText(
                        text = task.text,
                        color = Ink,
                        maxFontSize = 24.sp,
                        minFontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CompleteButton(enabled = !completing) { completing = true }
                }
            }

            if (stamp.value > 0f) StampOverlay(stamp.value)
        }

        // 상단 중앙 압정 (탭 = 고정/해제)
        Icon(
            imageVector = if (task.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
            contentDescription = if (task.pinned) "고정 해제" else "고정",
            tint = if (task.pinned) PinRed else Ink.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(26.dp)
                .graphicsLayer { rotationZ = if (task.pinned) 0f else 28f }
                .clickable(enabled = !completing) { onTogglePin() },
        )
    }
}

@Composable
private fun DateRow(task: Task) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // 작성일 — 좌측 정렬
        Icon(Icons.Rounded.EditNote, null, tint = Ink.copy(alpha = 0.5f), modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(3.dp))
        Text(
            formatShortDow(task.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = Ink.copy(alpha = 0.6f),
        )
        if (task.dueDate != null) {
            // 마감일 — 우측 정렬
            Spacer(Modifier.weight(1f))
            val days = daysUntilDue(task.dueDate)
            val dueColor = when {
                days == null -> Ink
                days < 0 -> PinRed
                days <= 1 -> MintDark
                else -> Ink.copy(alpha = 0.6f)
            }
            Icon(Icons.Rounded.Schedule, null, tint = dueColor, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(3.dp))
            Text(
                dueLabel(task.dueDate) ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = dueColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CompleteButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.55f))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Check, "완료", tint = MintDark, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun StampOverlay(p: Float) {
    Canvas(Modifier.fillMaxSize()) {
        // 살짝 덮이는 민트 톤
        drawRect(Mint.copy(alpha = 0.10f * p.coerceAtMost(1f)))
        // 줄긋기
        val midY = size.height * 0.46f
        val startX = size.width * 0.16f
        val endX = startX + size.width * 0.68f * p.coerceAtMost(1f)
        drawLine(Ink.copy(alpha = 0.65f), Offset(startX, midY), Offset(endX, midY), strokeWidth = 3.dp.toPx())
        // 체크 도장
        val cx = size.width / 2f
        val cy = size.height * 0.5f
        val r = size.minDimension * 0.20f * p
        drawCircle(MintDark, radius = r, center = Offset(cx, cy), style = Stroke(width = 4.dp.toPx()))
        val check = Path().apply {
            moveTo(cx - r * 0.45f, cy + r * 0.02f)
            lineTo(cx - r * 0.10f, cy + r * 0.40f)
            lineTo(cx + r * 0.50f, cy - r * 0.42f)
        }
        drawPath(check, MintDark, style = Stroke(width = 5.dp.toPx()))
    }
}
