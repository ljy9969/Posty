package com.bimatrix.posty.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bimatrix.posty.data.PostyStore
import com.bimatrix.posty.data.TaskSideEffects

/**
 * Android 진입점 — 앱 모듈은 Compose 를 직접 다루지 않고 이 확장만 호출한다.
 * (모든 Compose 의존/버전은 shared 모듈 안에서 일원화)
 */
fun ComponentActivity.setPostyContent(store: PostyStore, sideEffects: TaskSideEffects) {
    setContent {
        PostyRoot(store = store, sideEffects = sideEffects)
    }
}
