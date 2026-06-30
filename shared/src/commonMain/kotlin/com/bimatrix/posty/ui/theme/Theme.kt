package com.bimatrix.posty.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Posty 는 의도적으로 항상 밝고 상큼한 라이트 테마를 유지한다.
private val PostyColorScheme = lightColorScheme(
    primary = Mint,
    onPrimary = Cream,
    primaryContainer = MintSoft,
    onPrimaryContainer = MintDark,
    secondary = MintDark,
    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = CreamDim,
    onSurfaceVariant = InkSoft,
    outline = InkSoft,
)

/**
 * 공용 테마. 시스템 바 색 등 플랫폼별 처리는 각 플랫폼 진입점에서 한다
 * (Android: Activity window, iOS: 상태바 스타일).
 */
@Composable
fun PostyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PostyColorScheme,
        typography = PostyTypography,
        content = content,
    )
}
