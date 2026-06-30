package com.bimatrix.posty.data

/**
 * 저장 변경에 따른 플랫폼 부수효과(알림 예약/취소, 위젯 갱신).
 * Android: AlarmManager + 앱 위젯. iOS: UNUserNotificationCenter(위젯은 없음).
 */
interface TaskSideEffects {
    /** 미완료 + 마감일 있으면 알림 예약, 아니면 취소(구현이 판단). */
    fun scheduleReminder(task: Task)
    fun cancelReminder(taskId: String)
    /** 변경 후 전체 목록 — 위젯 등 외부 표시 갱신용. */
    fun onTasksChanged(tasks: List<Task>)

    /** 아무 동작도 하지 않는 기본 구현(테스트/초기 단계용). */
    companion object None : TaskSideEffects {
        override fun scheduleReminder(task: Task) {}
        override fun cancelReminder(taskId: String) {}
        override fun onTasksChanged(tasks: List<Task>) {}
    }
}
