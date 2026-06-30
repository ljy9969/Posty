package com.bimatrix.posty.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.bimatrix.posty.data.Task
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** 마감일 당일 오전 9시에 로컬 알림을 발화하도록 예약/취소한다. */
object ReminderScheduler {

    const val CHANNEL_ID = "posty_due_reminder"
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_TASK_TEXT = "task_text"
    private const val NOTIFY_HOUR = 9

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "마감 알림",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "할 일 마감일 당일 아침 알림" }
            manager.createNotificationChannel(channel)
        }
    }

    /** dueDate(UTC 자정)를 시스템 시간대의 마감 당일 오전 9시로 변환. 과거면 null. */
    private fun triggerAt(dueDate: Long): Long? {
        val date = Instant.ofEpochMilli(dueDate).atZone(ZoneOffset.UTC).toLocalDate()
        val millis = date.atTime(NOTIFY_HOUR, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return if (millis <= System.currentTimeMillis()) null else millis
    }

    private fun pendingIntent(context: Context, task: Task): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TEXT, task.text)
        }
        return PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** 미완료 + 마감일 있음 → 예약. 그 외 → 취소. */
    fun schedule(context: Context, task: Task) {
        ensureChannel(context)
        val due = task.dueDate
        if (task.isCompleted || due == null) {
            cancel(context, task.id)
            return
        }
        val at = triggerAt(due) ?: return
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(context, task)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()
        try {
            if (canExact) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (_: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(context: Context, taskId: String) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarm.cancel(pi)
    }
}
