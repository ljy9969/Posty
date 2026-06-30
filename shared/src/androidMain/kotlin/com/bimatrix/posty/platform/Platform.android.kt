package com.bimatrix.posty.platform

import java.util.UUID

actual fun randomUuid(): String = UUID.randomUUID().toString()

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
