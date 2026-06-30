package com.bimatrix.posty.data

import kotlinx.coroutines.flow.Flow

/**
 * 영속 저장소 추상화. 단일 단말 로컬 저장(계정/네트워크 없음).
 * Android 는 DataStore Preferences, iOS 는 NSUserDefaults 로 구현한다.
 *
 * [editTasks] 는 '읽기→변형→쓰기'를 원자적으로 수행해야 한다(동시 변경 안전).
 */
interface PostyStore {
    val tasksJson: Flow<String?>
    val freeMode: Flow<Boolean>
    val deckMode: Flow<Boolean>
    val freeZoom: Flow<Float>
    val lineZoom: Flow<Float>

    /** 현재 tasks JSON(없으면 null)을 받아 새 JSON 을 반환하면 원자적으로 저장. */
    suspend fun editTasks(transform: (String?) -> String)

    suspend fun setFreeMode(value: Boolean)
    suspend fun setDeckMode(value: Boolean)
    suspend fun setFreeZoom(value: Float)
    suspend fun setLineZoom(value: Float)
}
