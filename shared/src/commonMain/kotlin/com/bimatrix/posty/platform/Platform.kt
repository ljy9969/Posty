package com.bimatrix.posty.platform

/** 플랫폼별 난수 UUID 문자열(Android: java.util.UUID, iOS: NSUUID). */
expect fun randomUuid(): String

/** 현재 시각(epoch milliseconds). Android: System, iOS: NSDate. */
expect fun currentTimeMillis(): Long
