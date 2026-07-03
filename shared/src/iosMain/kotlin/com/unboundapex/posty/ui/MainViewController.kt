package com.unboundapex.posty.ui

import androidx.compose.ui.window.ComposeUIViewController
import com.unboundapex.posty.data.IosPostyStore
import com.unboundapex.posty.reminder.IosReminders
import platform.UIKit.UIViewController

// 앱 1회 생성: 저장소(NSUserDefaults 미러링) + 마감 알림(UNUserNotificationCenter).
private val iosStore = IosPostyStore()
private val iosReminders = IosReminders()

/**
 * iOS 진입점 — Swift(iosApp) 에서 이 UIViewController 를 호스팅한다.
 */
fun MainViewController(): UIViewController {
    iosReminders.requestAuthorization()
    return ComposeUIViewController {
        PostyRoot(store = iosStore, sideEffects = iosReminders)
    }
}
