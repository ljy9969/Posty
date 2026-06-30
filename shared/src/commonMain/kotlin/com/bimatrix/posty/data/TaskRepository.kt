package com.bimatrix.posty.data

import com.bimatrix.posty.platform.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.hypot

/** 겹침 그룹으로 묶이는 위치 임계값(dp, 좌상단 기준 거리). */
private const val GROUP_THRESHOLD = 120f

/** 묶음이 부채꼴로 펼쳐지는 간격(UI 의 FAN_X/FAN_Y 와 일치) — 겹침 판정 시 펼친 범위를 고려. */
private const val GROUP_FAN_X = 74f
private const val GROUP_FAN_Y = 14f

/** 한 묶음(스택)에 들어갈 수 있는 최대 카드 수. */
private const val MAX_GROUP = 4

/** 자유 보드 되돌리기 스택 최대 깊이. */
private const val UNDO_LIMIT = 20

private fun nowMillis(): Long = currentTimeMillis()

/**
 * 할 일 저장소. 영속은 [PostyStore](플랫폼별), 알림/위젯 등 부수효과는 [TaskSideEffects](플랫폼별)로 위임.
 * 비즈니스 로직(우선순위·고정·그룹·되돌리기)은 모두 공용(commonMain)이다.
 */
class TaskRepository(
    private val store: PostyStore,
    private val sideEffects: TaskSideEffects = TaskSideEffects.None,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** 자유 보드 배치 되돌리기용 인메모리 스냅샷 스택(변경 직전 상태). */
    private val undoStack = ArrayDeque<List<Task>>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: Flow<Boolean> = _canUndo

    /** 방금 삭제한 항목(스낵바 '실행취소' 전용 — 다른 동작에 영향받지 않는 독립 슬롯). */
    private var lastDeleted: Task? = null

    val tasks: Flow<List<Task>> = store.tasksJson.map { decode(it) }

    val freeMode: Flow<Boolean> = store.freeMode

    /** 라인 화면 표현: true = 깊이감 덱(스택), false = 가로 평면 펼침. */
    val deckMode: Flow<Boolean> = store.deckMode

    /** 줌 비율(앱을 다시 열어도 배치 보던 그대로 유지). */
    val freeZoom: Flow<Float> = store.freeZoom
    val lineZoom: Flow<Float> = store.lineZoom

    suspend fun setDeckMode(enabled: Boolean) = store.setDeckMode(enabled)
    suspend fun setFreeMode(enabled: Boolean) = store.setFreeMode(enabled)
    suspend fun setFreeZoom(z: Float) = store.setFreeZoom(z)
    suspend fun setLineZoom(z: Float) = store.setLineZoom(z)

    private fun decode(raw: String?): List<Task> =
        raw?.let { runCatching { json.decodeFromString<List<Task>>(it) }.getOrNull() } ?: emptyList()

    private fun pushUndo(snapshot: List<Task>) {
        if (undoStack.lastOrNull() == snapshot) return
        undoStack.addLast(snapshot)
        while (undoStack.size > UNDO_LIMIT) undoStack.removeFirst()
        _canUndo.value = true
    }

    private fun clearUndo() {
        if (undoStack.isNotEmpty()) undoStack.clear()
        _canUndo.value = false
    }

    /** 변경 후 결과 리스트를 받아 위젯 동기화에 사용. 되돌리기 스택은 비운다. */
    private suspend fun mutate(transform: (List<Task>) -> List<Task>): List<Task> {
        var result: List<Task> = emptyList()
        store.editTasks { raw ->
            clearUndo()
            result = transform(decode(raw))
            json.encodeToString(result)
        }
        sideEffects.onTasksChanged(result)
        return result
    }

    /** 자유 보드 배치 변경 — 직전 상태를 되돌리기 스택에 쌓는다. */
    private suspend fun mutateUndoable(transform: (List<Task>) -> List<Task>): List<Task> {
        var result: List<Task> = emptyList()
        store.editTasks { raw ->
            val current = decode(raw)
            pushUndo(current)
            result = transform(current)
            json.encodeToString(result)
        }
        sideEffects.onTasksChanged(result)
        return result
    }

    /** 직전 변경(배치·묶기·해제·삭제)을 되돌린다. */
    suspend fun undo() {
        if (undoStack.isEmpty()) return
        val snapshot = undoStack.removeLast().also { _canUndo.value = undoStack.isNotEmpty() }
        store.editTasks { json.encodeToString(snapshot) }
        // 삭제를 되돌린 경우 등, 복구된 활성 항목의 마감 알림을 다시 건다(중복 스케줄은 갱신됨).
        snapshot.filter { !it.isCompleted && it.dueDate != null }
            .forEach { sideEffects.scheduleReminder(it) }
        sideEffects.onTasksChanged(snapshot)
    }

    private fun syncReminder(task: Task?) {
        if (task != null) sideEffects.scheduleReminder(task)
    }

    suspend fun add(text: String, dueDate: Long?, colorIndex: Int) {
        var created: Task? = null
        mutate { list ->
            val active = list.filter { !it.isCompleted }
            val nextOrder = (active.maxOfOrNull { it.order } ?: -1) + 1
            val count = active.size
            val task = Task(
                text = text.trim(),
                createdAt = nowMillis(),
                dueDate = dueDate,
                colorIndex = colorIndex,
                order = nextOrder,
                posX = 16f + (count % 3) * 124f,
                posY = 16f + (count / 3) * 150f,
            )
            created = task
            list + task
        }
        syncReminder(created)
    }

    suspend fun update(id: String, text: String, dueDate: Long?, colorIndex: Int) {
        val result = mutate { list ->
            list.map {
                if (it.id == id) it.copy(text = text.trim(), dueDate = dueDate, colorIndex = colorIndex) else it
            }
        }
        syncReminder(result.firstOrNull { it.id == id })
    }

    /**
     * 삭제 — 두 경로로 복구 가능:
     * 1) 스낵바 '실행취소' → 전용 슬롯(lastDeleted): 다른 동작에 영향받지 않음.
     * 2) 자유 화면 되돌리기 버튼 → 되돌리기 스택(mutateUndoable): 스낵바가 사라진 뒤에도 복구.
     */
    suspend fun delete(id: String) {
        var removed: Task? = null
        mutateUndoable { list ->
            removed = list.firstOrNull { it.id == id }
            list.filterNot { it.id == id }
        }
        lastDeleted = removed
        sideEffects.cancelReminder(id)
    }

    /** 방금 삭제한 항목을 그대로 되살린다(완료/미완료·위치·마감 유지). */
    suspend fun restoreLastDeleted() {
        val task = lastDeleted ?: return
        lastDeleted = null
        val result = mutate { list -> if (list.any { it.id == task.id }) list else list + task }
        if (!task.isCompleted && task.dueDate != null) {
            syncReminder(result.firstOrNull { it.id == task.id })
        }
    }

    suspend fun complete(id: String) {
        mutate { list ->
            list.map { if (it.id == id) it.copy(completedAt = nowMillis(), groupId = null) else it }
        }
        sideEffects.cancelReminder(id)
    }

    suspend fun restore(id: String) {
        val result = mutate { list ->
            val nextOrder = (list.filter { !it.isCompleted }.maxOfOrNull { it.order } ?: -1) + 1
            list.map { if (it.id == id) it.copy(completedAt = null, pinned = false, order = nextOrder) else it }
        }
        syncReminder(result.firstOrNull { it.id == id })
    }

    suspend fun togglePin(id: String) {
        mutate { list ->
            val target = list.firstOrNull { it.id == id } ?: return@mutate list
            if (target.pinned) {
                // 해제 시 고정 구역을 벗어나도록 맨 뒤 우선순위로.
                val maxOrder = (list.filter { !it.isCompleted }.maxOfOrNull { it.order } ?: -1) + 1
                list.map { if (it.id == id) it.copy(pinned = false, order = maxOrder) else it }
            } else {
                val minOrder = (list.filter { !it.isCompleted }.minOfOrNull { it.order } ?: 0) - 1
                list.map { if (it.id == id) it.copy(pinned = true, order = minOrder) else it }
            }
        }
    }

    /** 드래그(라인 모드) 재배치. orderedIds 좌→우. */
    suspend fun reorderActive(orderedIds: List<String>) {
        mutate { list ->
            val orderMap = orderedIds.withIndex().associate { (index, id) -> id to index }
            list.map { t -> orderMap[t.id]?.let { t.copy(order = it) } ?: t }
        }
    }

    /** 자유 보드: 노트를 (x,y)로 옮기고, '옮긴 그 카드'만 겹침에 따라 그룹에 넣거나 뺀다. */
    suspend fun moveNote(id: String, x: Float, y: Float) {
        mutateUndoable { list ->
            val moved = list.map { if (it.id == id) it.copy(posX = x, posY = y) else it }
            regroupAfterMove(moved, id)
        }
    }

    /**
     * 그룹 해제: 지정한 노트들의 groupId 를 지우고 받은 위치로 흩어 놓는다.
     * 위치가 겹침 임계값보다 충분히 벌어져 있으므로 재그룹을 돌리지 않는다(되돌리기 가능).
     */
    suspend fun ungroup(positions: List<Triple<String, Float, Float>>) {
        if (positions.isEmpty()) return
        val posMap = positions.associate { it.first to (it.second to it.third) }
        mutateUndoable { list ->
            list.map { t ->
                posMap[t.id]?.let { t.copy(posX = it.first, posY = it.second, groupId = null) } ?: t
            }
        }
    }

    /**
     * 그룹을 통째로 이동. 위치만 갱신하고 자동 재그룹은 하지 않는다(이미 묶인 묶음은 그대로 유지,
     * 다른 카드를 빨아들이지 않음). 새 그룹은 단일 카드를 끌어다 겹칠 때(moveNote)만 만들어진다.
     */
    suspend fun moveNotes(positions: List<Triple<String, Float, Float>>) {
        if (positions.isEmpty()) return
        val posMap = positions.associate { it.first to (it.second to it.third) }
        mutateUndoable { list ->
            list.map { t ->
                posMap[t.id]?.let { t.copy(posX = it.first, posY = it.second) } ?: t
            }
        }
    }

    /**
     * '방금 옮긴 한 카드'에 대해서만 그룹 소속을 다시 정한다(전체 재그룹 아님).
     * - 겹치는 다른 카드가 없으면 → 단독(groupId=null).
     * - 겹치는 카드가 그룹이면 → 그 그룹에 합류(최대 [MAX_GROUP] 까지).
     * - 겹치는 카드가 단독이면 → 둘이 새 그룹.
     * - 빠져나온 기존 그룹이 1장만 남으면 해체.
     * 다른 카드들의 묶음은 건드리지 않으므로, 해제한 카드가 멋대로 다시 묶이지 않는다.
     */
    private fun regroupAfterMove(list: List<Task>, movedId: String): List<Task> {
        val active = list.filter { !it.isCompleted }
        val moved = active.firstOrNull { it.id == movedId } ?: return list
        val mx = moved.posX ?: 0f
        val my = moved.posY ?: 0f

        // 후보 카드(그 카드가 속한 묶음의 '부채꼴 박스')까지의 거리.
        // 묶음 멤버는 모두 앵커 한 점에 저장되지만 화면엔 부채꼴로 펼쳐지므로, 펼친 범위 전체로 판정.
        fun reach(c: Task): Float {
            val gsize = if (c.groupId != null) active.count { it.groupId == c.groupId } else 1
            val ax = c.posX ?: 0f
            val ay = c.posY ?: 0f
            val fanW = (gsize - 1) * GROUP_FAN_X
            val fanH = (gsize - 1) * GROUP_FAN_Y
            val dx = maxOf(ax - mx, 0f, mx - (ax + fanW))
            val dy = maxOf(ay - my, 0f, my - (ay + fanH))
            return hypot(dx, dy)
        }

        val oldGid = moved.groupId
        val nearest = active
            .filter { it.id != movedId && reach(it) < GROUP_THRESHOLD }
            .minByOrNull { reach(it) }

        var newGid: String? = null
        if (nearest != null) {
            val targetGid = nearest.groupId
            newGid = if (targetGid != null) {
                val groupSize = active.count { it.groupId == targetGid }
                if (groupSize < MAX_GROUP) targetGid else null
            } else {
                minOf(movedId, nearest.id)
            }
        }

        val groupUpdates = HashMap<String, String?>()
        groupUpdates[movedId] = newGid
        // 단독 카드와 새로 짝지으면 그 카드도 같은 그룹으로.
        if (newGid != null && nearest != null && nearest.groupId == null) {
            groupUpdates[nearest.id] = newGid
        }
        // 기존 그룹을 떠났고 남은 멤버가 1장뿐이면 그 그룹도 해체.
        if (oldGid != null && newGid != oldGid) {
            val remaining = active.filter { it.groupId == oldGid && it.id != movedId }
            if (remaining.size == 1) groupUpdates[remaining[0].id] = null
        }

        // 합류 시 옮긴 카드를 묶음 앵커 좌표로 스냅 → 모든 멤버가 같은 좌표(함께 이동 일관성).
        val snapX = if (newGid != null && nearest != null) nearest.posX else null
        val snapY = if (newGid != null && nearest != null) nearest.posY else null

        return list.map { t ->
            when {
                t.id == movedId && newGid != null ->
                    t.copy(groupId = newGid, posX = snapX ?: t.posX, posY = snapY ?: t.posY)
                groupUpdates.containsKey(t.id) -> t.copy(groupId = groupUpdates[t.id])
                else -> t
            }
        }
    }
}
