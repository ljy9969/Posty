package com.bimatrix.posty.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 프로세스 당 단일 DataStore 인스턴스 (앱 + 위젯 공용). */
private val Context.postyDataStore by preferencesDataStore(name = "posty")

/** Android 영속 — DataStore Preferences. 키 이름은 기존 앱과 동일(데이터 이전 호환). */
class AndroidPostyStore(private val context: Context) : PostyStore {
    private val tasksKey = stringPreferencesKey("tasks_json")
    private val freeModeKey = booleanPreferencesKey("free_mode")
    private val deckModeKey = booleanPreferencesKey("deck_mode")
    private val freeZoomKey = floatPreferencesKey("free_zoom")
    private val lineZoomKey = floatPreferencesKey("line_zoom")

    override val tasksJson: Flow<String?> = context.postyDataStore.data.map { it[tasksKey] }
    override val freeMode: Flow<Boolean> = context.postyDataStore.data.map { it[freeModeKey] ?: false }
    override val deckMode: Flow<Boolean> = context.postyDataStore.data.map { it[deckModeKey] ?: true }
    override val freeZoom: Flow<Float> = context.postyDataStore.data.map { it[freeZoomKey] ?: 1f }
    override val lineZoom: Flow<Float> = context.postyDataStore.data.map { it[lineZoomKey] ?: 1f }

    override suspend fun editTasks(transform: (String?) -> String) {
        context.postyDataStore.edit { it[tasksKey] = transform(it[tasksKey]) }
    }

    override suspend fun setFreeMode(value: Boolean) {
        context.postyDataStore.edit { it[freeModeKey] = value }
    }

    override suspend fun setDeckMode(value: Boolean) {
        context.postyDataStore.edit { it[deckModeKey] = value }
    }

    override suspend fun setFreeZoom(value: Float) {
        context.postyDataStore.edit { it[freeZoomKey] = value }
    }

    override suspend fun setLineZoom(value: Float) {
        context.postyDataStore.edit { it[lineZoomKey] = value }
    }
}
