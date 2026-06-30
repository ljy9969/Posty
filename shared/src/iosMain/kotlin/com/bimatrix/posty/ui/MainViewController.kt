package com.bimatrix.posty.ui

import androidx.compose.ui.window.ComposeUIViewController
import com.bimatrix.posty.data.IosPostyStore
import platform.UIKit.UIViewController

// 앱 1회 생성되는 저장소(NSUserDefaults 미러링).
private val iosStore = IosPostyStore()

/**
 * iOS 진입점 — Swift(iosApp) 에서 이 UIViewController 를 호스팅한다.
 * 마감 알림(UNUserNotificationCenter)은 iOS 빌드가 CI 에서 통과한 뒤 추가 예정 → 지금은 무동작.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    PostyRoot(store = iosStore)
}
