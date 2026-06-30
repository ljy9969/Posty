package com.bimatrix.posty.ui

import com.bimatrix.posty.data.Task
import com.bimatrix.posty.data.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 화면 상태/액션 보유. 멀티플랫폼이라 AndroidViewModel 대신 일반 클래스 —
 * 앱 루트에서 한 번 생성해 앱 생명주기 동안 유지되는 [scope] 를 주입받는다.
 */
class PostyViewModel(
    private val repo: TaskRepository,
    private val scope: CoroutineScope,
) {

    /** 저장소에서 한 번이라도 데이터를 읽었는지. 첫 프레임의 '빈 화면 점멸'을 막는 데 사용. */
    val isLoaded: StateFlow<Boolean> = repo.tasks
        .map { true }
        .stateIn(scope, SharingStarted.Eagerly, false)

    /** 보드: 미완료 할 일. 고정(pinned)이 항상 맨 앞, 그다음 우선순위(order). */
    val activeTasks: StateFlow<List<Task>> = repo.tasks
        .map { all ->
            all.filter { !it.isCompleted }
                .sortedWith(compareByDescending<Task> { it.pinned }.thenBy { it.order })
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** 완료 보관함: 최근 완료 순. */
    val completedTasks: StateFlow<List<Task>> = repo.tasks
        .map { all -> all.filter { it.isCompleted }.sortedByDescending { it.completedAt ?: 0L } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** 보드 모드: false = 라인(우선순위), true = 자유 배치. */
    val freeMode: StateFlow<Boolean> = repo.freeMode
        .stateIn(scope, SharingStarted.Eagerly, false)

    /** 라인 표현: true = 깊이감 덱, false = 가로 펼침. */
    val deckMode: StateFlow<Boolean> = repo.deckMode
        .stateIn(scope, SharingStarted.Eagerly, true)

    fun setDeckMode(enabled: Boolean) = scope.launch { repo.setDeckMode(enabled) }

    /** 자유 보드에서 되돌릴 직전 배치가 있는지. */
    val canUndo: StateFlow<Boolean> = repo.canUndo
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), false)

    /** 줌 비율(저장됨 — 다시 열어도 유지). */
    val freeZoom: StateFlow<Float> = repo.freeZoom
        .stateIn(scope, SharingStarted.Eagerly, 1f)
    val lineZoom: StateFlow<Float> = repo.lineZoom
        .stateIn(scope, SharingStarted.Eagerly, 1f)

    fun setFreeZoom(z: Float) = scope.launch { repo.setFreeZoom(z) }
    fun setLineZoom(z: Float) = scope.launch { repo.setLineZoom(z) }

    fun setFreeMode(enabled: Boolean) = scope.launch { repo.setFreeMode(enabled) }

    fun moveNote(id: String, x: Float, y: Float) = scope.launch { repo.moveNote(id, x, y) }

    /** 그룹 전체를 한 번에 이동. (id, x, y) 목록. */
    fun moveNotes(positions: List<Triple<String, Float, Float>>) = scope.launch {
        repo.moveNotes(positions)
    }

    /** 그룹 해제 — 멤버들을 벌려 놓으며 묶음을 푼다. */
    fun ungroup(positions: List<Triple<String, Float, Float>>) = scope.launch {
        repo.ungroup(positions)
    }

    /** 직전 자유 보드 배치를 되돌린다. */
    fun undo() = scope.launch { repo.undo() }

    fun addTask(text: String, dueDate: Long?, colorIndex: Int) = scope.launch {
        repo.add(text, dueDate, colorIndex)
    }

    fun updateTask(id: String, text: String, dueDate: Long?, colorIndex: Int) = scope.launch {
        repo.update(id, text, dueDate, colorIndex)
    }

    fun completeTask(id: String) = scope.launch { repo.complete(id) }

    fun restoreTask(id: String) = scope.launch { repo.restore(id) }

    fun deleteTask(id: String) = scope.launch { repo.delete(id) }

    /** 방금 삭제한 항목 복구(스낵바 실행취소). */
    fun undoDelete() = scope.launch { repo.restoreLastDeleted() }

    fun togglePin(id: String) = scope.launch { repo.togglePin(id) }

    fun reorderActive(orderedIds: List<String>) = scope.launch { repo.reorderActive(orderedIds) }
}
