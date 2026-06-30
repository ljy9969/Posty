package com.bimatrix.posty.ui.theme

import androidx.compose.ui.graphics.Color

// ── Mint & Cream 브랜드 컬러 ───────────────────────────────
val Cream = Color(0xFFFFFDF7)       // 배경
val CreamDim = Color(0xFFF3EFE4)    // 카드/표면 보조
val Mint = Color(0xFF4ECDC4)        // 포인트
val MintDark = Color(0xFF2BB3AA)
val MintSoft = Color(0xFFDFF6F3)    // 민트 옅은 톤(선택 강조 등)
val Ink = Color(0xFF3A3A3A)         // 본문 텍스트
val InkSoft = Color(0xFF8A8A8A)     // 보조 텍스트
val Lavender = Color(0xFF8E74CC)    // 보조 포인트(완료 칩)
val LavenderSoft = Color(0xFFEDE7F8) // 완료 칩 배경

/**
 * 포스트잇 색상 팔레트. Task.colorIndex 가 이 배열을 가리킨다.
 * (배경색, 접힌-모서리 그늘색) 쌍 — 아날로그 종이 느낌을 위해 약간 더 진한 그늘색을 둠.
 */
val StickyPalette: List<Pair<Color, Color>> = listOf(
    Color(0xFFFFF3B0) to Color(0xFFF2E08C), // 레몬
    Color(0xFFC7F0DB) to Color(0xFFA6E3C4), // 민트그린
    Color(0xFFFFD8CC) to Color(0xFFF7BCAB), // 복숭아
    Color(0xFFD6E8FF) to Color(0xFFB9D6FB), // 하늘
    Color(0xFFE8DFF5) to Color(0xFFD3C5EC), // 라벤더
    Color(0xFFFFE3F1) to Color(0xFFF8C6E0), // 핑크
)

fun stickyColor(index: Int): Color = StickyPalette[index.mod(StickyPalette.size)].first
fun stickyShade(index: Int): Color = StickyPalette[index.mod(StickyPalette.size)].second
