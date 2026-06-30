package com.bimatrix.posty.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimatrix.posty.data.Task
import com.bimatrix.posty.ui.formatFull
import com.bimatrix.posty.ui.theme.Cream
import com.bimatrix.posty.ui.theme.Ink
import com.bimatrix.posty.ui.theme.InkSoft
import com.bimatrix.posty.ui.theme.Mint
import com.bimatrix.posty.ui.theme.MintDark
import com.bimatrix.posty.ui.theme.StickyPalette
import com.bimatrix.posty.ui.theme.stickyColor

/**
 * 전체 화면 편집기 — 포스트잇을 뒤집어 자세히 적는 "Note-ception" 화면.
 * [existing] 이 null 이면 새 할 일, 아니면 수정.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    existing: Task?,
    onSave: (text: String, dueDate: Long?, colorIndex: Int) -> Unit,
    onDelete: (() -> Unit)?,
    onClose: () -> Unit,
) {
    var text by remember { mutableStateOf(existing?.text ?: "") }
    var dueDate by remember { mutableStateOf(existing?.dueDate) }
    var colorIndex by remember { mutableIntStateOf(existing?.colorIndex ?: 0) }
    var showPicker by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(20.dp),
    ) {
        // 상단 바
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, "닫기", tint = Ink)
            }
            Spacer(Modifier.weight(1f))
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, "삭제", tint = Color(0xFFE5604D))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 확대된 포스트잇 (뒷면)
        Box(
            Modifier
                .fillMaxWidth()
                .height(260.dp)
                .shadow(8.dp, RoundedCornerShape(14.dp), clip = false)
                .clip(RoundedCornerShape(14.dp))
                .background(stickyColor(colorIndex))
                .padding(18.dp),
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text("할 일을 한 줄로 적어보세요", color = Ink.copy(alpha = 0.4f), fontSize = 20.sp)
                },
                textStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, color = Ink),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MintDark,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(Modifier.height(20.dp))

        Text("색상", style = MaterialTheme.typography.labelSmall, color = InkSoft)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StickyPalette.forEachIndexed { i, _ ->
                val selected = i == colorIndex
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(stickyColor(i))
                        .then(
                            if (selected) Modifier.border(3.dp, MintDark, CircleShape)
                            else Modifier.border(1.dp, Ink.copy(alpha = 0.1f), CircleShape),
                        )
                        .clickable { colorIndex = i },
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text("마감일", style = MaterialTheme.typography.labelSmall, color = InkSoft)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Ink.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .clickable { showPicker = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Event, null, tint = MintDark, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        dueDate?.let { formatFull(it) } ?: "마감 없음",
                        color = if (dueDate != null) Ink else InkSoft,
                    )
                }
            }
            if (dueDate != null) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { dueDate = null }) { Text("지우기", color = InkSoft) }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { if (text.isNotBlank()) onSave(text, dueDate, colorIndex) },
            enabled = text.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            Text(if (existing == null) "붙이기" else "저장", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dueDate)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = state.selectedDateMillis
                    showPicker = false
                }) { Text("확인", color = MintDark) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("취소", color = InkSoft) }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
