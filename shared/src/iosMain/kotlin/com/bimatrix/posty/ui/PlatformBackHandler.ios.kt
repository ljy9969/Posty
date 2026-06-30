package com.bimatrix.posty.ui

import androidx.compose.runtime.Composable

/** iOS는 네이티브 가장자리 스와이프/내비게이션이 뒤로가기를 담당 — 별도 가로채기 없음. */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
}
