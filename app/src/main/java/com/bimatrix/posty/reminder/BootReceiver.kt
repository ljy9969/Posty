package com.bimatrix.posty.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bimatrix.posty.data.AndroidPostyStore
import com.bimatrix.posty.data.TaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** 재부팅 시 예약 알람이 사라지므로, 미완료+마감 할 일들의 알림을 다시 예약한다. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val tasks = runBlocking { TaskRepository(AndroidPostyStore(context)).tasks.first() }
        tasks.filter { !it.isCompleted && it.dueDate != null }
            .forEach { ReminderScheduler.schedule(context, it) }
    }
}
