package com.bimatrix.posty.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimatrix.posty.data.PostyStore
import com.bimatrix.posty.data.Task
import com.bimatrix.posty.data.TaskRepository
import com.bimatrix.posty.data.TaskSideEffects
import com.bimatrix.posty.ui.board.BoardScreen
import com.bimatrix.posty.ui.completed.CompletedScreen
import com.bimatrix.posty.ui.edit.EditTaskScreen
import com.bimatrix.posty.ui.theme.Cream
import com.bimatrix.posty.ui.theme.Ink
import com.bimatrix.posty.ui.theme.Mint
import com.bimatrix.posty.ui.theme.PostyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 플랫폼 공용 앱 루트. 각 플랫폼(Android Activity / iOS UIViewController)에서
 * 자기 [store] 와 [sideEffects] 를 넣어 호출한다. 저장소·뷰모델은 여기서 한 번 생성된다.
 */
@Composable
fun PostyRoot(store: PostyStore, sideEffects: TaskSideEffects = TaskSideEffects.None) {
    val scope = rememberCoroutineScope()
    val vm = remember { PostyViewModel(TaskRepository(store, sideEffects), scope) }
    PostyTheme {
        Surface(Modifier.fillMaxSize(), color = Cream) {
            PostyApp(vm)
        }
    }
}

private sealed interface Screen {
    data object Board : Screen
    data object Completed : Screen
    data class Edit(val task: Task?) : Screen
}

@Composable
private fun PostyApp(vm: PostyViewModel) {
    val active by vm.activeTasks.collectAsState()
    val completed by vm.completedTasks.collectAsState()
    val freeMode by vm.freeMode.collectAsState()
    val canUndo by vm.canUndo.collectAsState()
    val isLoaded by vm.isLoaded.collectAsState()
    val deckMode by vm.deckMode.collectAsState()
    // 줌 비율은 저장소에 저장 — 화면 이동은 물론 앱을 다시 열어도 유지.
    val lineZoom by vm.lineZoom.collectAsState()
    val freeZoom by vm.freeZoom.collectAsState()
    var screen by remember { mutableStateOf<Screen>(Screen.Board) }

    // 시스템 뒤로가기(스와이프) — 보드가 아닌 화면에서는 앱을 닫지 않고 보드로 돌아간다.
    // (완료한 일 → 오늘의 보드, 카드 편집 → 오늘의 보드)
    PlatformBackHandler(enabled = screen !is Screen.Board) { screen = Screen.Board }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // 삭제 시 '실행취소' 스낵바로 즉시 복구. 표시 시간 7초(무기한으로 띄우고 7초 뒤 닫음).
    val deleteWithUndo: (String) -> Unit = { id ->
        vm.deleteTask(id)
        scope.launch {
            val timeout = launch {
                delay(7000)
                snackbarHostState.currentSnackbarData?.dismiss()
            }
            val result = snackbarHostState.showSnackbar(
                message = "할 일을 삭제했어요",
                actionLabel = "실행취소",
                duration = SnackbarDuration.Indefinite,
            )
            timeout.cancel()
            if (result == SnackbarResult.ActionPerformed) vm.undoDelete()
        }
    }

    val content: @Composable () -> Unit = {
        when (val s = screen) {
            is Screen.Board -> BoardScreen(
                tasks = active,
                completedCount = completed.size,
                freeMode = freeMode,
                deckMode = deckMode,
                onDeckChange = { vm.setDeckMode(it) },
                canUndo = canUndo,
                lineZoom = lineZoom,
                onLineZoomChange = { vm.setLineZoom(it) },
                freeZoom = freeZoom,
                onFreeZoomChange = { vm.setFreeZoom(it) },
                onTapTask = { screen = Screen.Edit(it) },
                onCompleteTask = { vm.completeTask(it) },
                onTogglePin = { vm.togglePin(it) },
                onReorder = { vm.reorderActive(it) },
                onMoveNote = { id, x, y -> vm.moveNote(id, x, y) },
                onMoveNotes = { vm.moveNotes(it) },
                onUndo = { vm.undo() },
                onUngroup = { vm.ungroup(it) },
                onToggleMode = { vm.setFreeMode(!freeMode) },
                onAdd = { screen = Screen.Edit(null) },
                onOpenCompleted = { screen = Screen.Completed },
            )

            is Screen.Completed -> CompletedScreen(
                tasks = completed,
                onBack = { screen = Screen.Board },
                onRestore = { vm.restoreTask(it) },
                onDelete = { deleteWithUndo(it) },
            )

            is Screen.Edit -> EditTaskScreen(
                existing = s.task,
                onSave = { text, due, color ->
                    if (s.task == null) vm.addTask(text, due, color)
                    else vm.updateTask(s.task.id, text, due, color)
                    screen = Screen.Board
                },
                onDelete = s.task?.let { t -> { deleteWithUndo(t.id); screen = Screen.Board } },
                onClose = { screen = Screen.Board },
            )
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        // 데이터가 처음 로드되기 전에는 빈 보드를 그리지 않는다(점멸 방지).
        if (isLoaded) content()
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 12.dp, end = 12.dp, bottom = 80.dp),
        ) { data ->
            // 직접 그린 스낵바 — 어두운 배경에 흰 글자, 민트 실행취소.
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Ink)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    data.visuals.message,
                    color = Color.White,
                    fontSize = 14.sp,
                )
                data.visuals.actionLabel?.let { label ->
                    Spacer(Modifier.width(20.dp))
                    Text(
                        label,
                        color = Mint,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { data.performAction() }
                            .padding(vertical = 6.dp),
                    )
                }
            }
        }
    }
}
