@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.unboundapex.posty.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.backhandler.BackHandler

/**
 * iOS: Compose Multiplatform 의 BackHandler 가 좌측 가장자리 스와이프(시스템 뒤로가기 제스처)를
 * 가로채 [onBack] 을 호출한다. [enabled] 이 false 면 제스처를 가로채지 않는다.
 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
