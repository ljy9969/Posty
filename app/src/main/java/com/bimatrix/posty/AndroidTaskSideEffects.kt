package com.bimatrix.posty

import android.content.Context
import com.bimatrix.posty.data.Task
import com.bimatrix.posty.data.TaskSideEffects
import com.bimatrix.posty.reminder.ReminderScheduler
import com.bimatrix.posty.widget.PostyWidgetProvider

/** Android 부수효과: 마감 알림(AlarmManager) 예약/취소 + 홈 위젯 갱신. */
class AndroidTaskSideEffects(private val context: Context) : TaskSideEffects {
    override fun scheduleReminder(task: Task) = ReminderScheduler.schedule(context, task)
    override fun cancelReminder(taskId: String) = ReminderScheduler.cancel(context, taskId)
    override fun onTasksChanged(tasks: List<Task>) = PostyWidgetProvider.requestUpdate(context)
}
