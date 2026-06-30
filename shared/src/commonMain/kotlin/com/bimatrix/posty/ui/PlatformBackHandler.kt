package com.bimatrix.posty.ui

import androidx.compose.runtime.Composable

/**
 * 시스템 뒤로가기 처리(플랫폼 공용).
 * - Android: 제스처(가장자리 스와이프)·뒤로가기 버튼을 가로채 [onBack] 호출.
 * - iOS: 네이티브 스와이프/내비게이션이 담당(여기서는 동작 없음).
 *
 * [enabled] 이 false 면 가로채지 않아 기본 동작(예: 앱 종료)으로 넘어간다.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
