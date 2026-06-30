package com.bimatrix.posty.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

/**
 * 포스트잇 크기가 한정돼 있으므로, 주어진 영역에 들어갈 때까지 폰트를 자동으로 줄인다(Fit Text).
 * 영역을 부모(weight 등)로 고정한 Box 안에서 사용한다.
 */
@Composable
fun BoxScope.AutoResizeText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = 24.sp,
    minFontSize: TextUnit = 12.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    maxLines: Int = 8,
    align: TextAlign = TextAlign.Center,
    textDecoration: TextDecoration? = null,
) {
    var fontSize by remember(text) { mutableStateOf(maxFontSize) }
    var ready by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        lineHeight = fontSize * 1.25f,
        textAlign = align,
        textDecoration = textDecoration,
        maxLines = maxLines,
        softWrap = true,
        style = TextStyle(),
        modifier = modifier
            .align(Alignment.Center)
            // 폰트 확정 전에는 그리지 않아 깜빡임 방지.
            .drawWithContent { if (ready) drawContent() },
        onTextLayout = { result ->
            if ((result.didOverflowHeight || result.didOverflowWidth) &&
                fontSize.value > minFontSize.value
            ) {
                fontSize = (fontSize.value - 1f).coerceAtLeast(minFontSize.value).sp
            } else {
                ready = true
            }
        },
    )
}
