package com.unboundapex.posty.reminder

import com.unboundapex.posty.data.Task
import com.unboundapex.posty.data.TaskSideEffects
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeZoneForSecondsFromGMT
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

/**
 * iOS 마감 알림 — Android 의 [com.unboundapex.posty.reminder.ReminderScheduler] 대응.
 * 마감일(UTC 자정 저장) 당일의 '로컬 오전 9시' 에 로컬 알림을 예약/취소한다.
 */
class IosReminders : TaskSideEffects {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    /** 앱 시작 시 1회 — 알림 권한 요청(이미 결정됐으면 시스템이 무시). */
    fun requestAuthorization() {
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
        ) { _, _ -> }
    }

    override fun scheduleReminder(task: Task) {
        val due = task.dueDate
        if (task.isCompleted || due == null) {
            cancelReminder(task.id)
            return
        }

        // dueDate 는 UTC 자정 → 그 달력일(연·월·일)을 뽑아 '로컬 오전 9시' 매칭으로 발화.
        val utcCal = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)
        utcCal.timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)
        val date = NSDate.dateWithTimeIntervalSince1970(due / 1000.0)
        val units = NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay
        val comps = utcCal.components(units, fromDate = date)

        val fire = platform.Foundation.NSDateComponents()
        fire.year = comps.year
        fire.month = comps.month
        fire.day = comps.day
        fire.hour = 9
        fire.minute = 0

        val content = UNMutableNotificationContent()
        content.setTitle("Due today 📌")
        content.setBody(task.text)

        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(fire, repeats = false)
        val request = UNNotificationRequest.requestWithIdentifier(task.id, content, trigger)
        center.addNotificationRequest(request, withCompletionHandler = null)
    }

    override fun cancelReminder(taskId: String) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(taskId))
    }

    override fun onTasksChanged(tasks: List<Task>) {
        // 위젯 새로고침은 Swift(앱 백그라운드 전환 시 WidgetCenter)에서 처리한다.
        // (Kotlin/Native 공용 메타데이터에 WidgetKit 바인딩이 없어 Swift 쪽에서 호출)
    }
}
