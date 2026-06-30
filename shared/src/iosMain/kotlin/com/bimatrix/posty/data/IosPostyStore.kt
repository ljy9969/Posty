package com.bimatrix.posty.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.Foundation.NSUserDefaults

/**
 * iOS 영속 — NSUserDefaults. 인메모리 [MutableStateFlow] 로 미러링해 변경을 즉시 방출한다
 * (단일 단말·단일 프로세스 앱이라 외부 동시 변경 없음).
 */
class IosPostyStore : PostyStore {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val mutex = Mutex()

    private val tasksKey = "tasks_json"
    private val freeModeKey = "free_mode"
    private val deckModeKey = "deck_mode"
    private val freeZoomKey = "free_zoom"
    private val lineZoomKey = "line_zoom"

    private fun boolOr(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key) != null) defaults.boolForKey(key) else default

    private fun floatOr(key: String, default: Float): Float =
        if (defaults.objectForKey(key) != null) defaults.floatForKey(key) else default

    private val _tasksJson = MutableStateFlow(defaults.stringForKey(tasksKey))
    private val _freeMode = MutableStateFlow(boolOr(freeModeKey, false))
    private val _deckMode = MutableStateFlow(boolOr(deckModeKey, true))
    private val _freeZoom = MutableStateFlow(floatOr(freeZoomKey, 1f))
    private val _lineZoom = MutableStateFlow(floatOr(lineZoomKey, 1f))

    override val tasksJson: Flow<String?> = _tasksJson
    override val freeMode: Flow<Boolean> = _freeMode
    override val deckMode: Flow<Boolean> = _deckMode
    override val freeZoom: Flow<Float> = _freeZoom
    override val lineZoom: Flow<Float> = _lineZoom

    override suspend fun editTasks(transform: (String?) -> String) {
        mutex.withLock {
            val newValue = transform(_tasksJson.value)
            defaults.setObject(newValue, forKey = tasksKey)
            _tasksJson.value = newValue
        }
    }

    override suspend fun setFreeMode(value: Boolean) {
        defaults.setBool(value, forKey = freeModeKey)
        _freeMode.value = value
    }

    override suspend fun setDeckMode(value: Boolean) {
        defaults.setBool(value, forKey = deckModeKey)
        _deckMode.value = value
    }

    override suspend fun setFreeZoom(value: Float) {
        defaults.setFloat(value, forKey = freeZoomKey)
        _freeZoom.value = value
    }

    override suspend fun setLineZoom(value: Float) {
        defaults.setFloat(value, forKey = lineZoomKey)
        _lineZoom.value = value
    }
}
