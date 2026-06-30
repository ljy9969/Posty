package com.bimatrix.posty.data

import com.bimatrix.posty.platform.randomUuid
import kotlinx.serialization.Serializable

/**
 * 포스트잇 한 장 = 작게 쪼갠 할 일 하나.
 *
 * 보드(활성) 화면에서는 [order] 오름차순으로 좌→우 나열된다(좌측이 우선순위 높음).
 * 완료되면 [completedAt] 이 채워지고 완료 보관함으로 이동한다.
 */
@Serializable
data class Task(
    val id: String = randomUuid(),
    val text: String,
    val createdAt: Long,
    val dueDate: Long? = null,
    val completedAt: Long? = null,
    /** 포스트잇 색상 팔레트 인덱스 (StickyPalette). */
    val colorIndex: Int = 0,
    /** 압정으로 고정된 중요한 메모. 압정 비주얼 + 좌측 우선 배치. */
    val pinned: Boolean = false,
    /** 보드 내 우선순위 위치. 낮을수록 좌측(높은 우선순위). */
    val order: Int = 0,
    /** 자유 배치 보드에서의 위치(dp, 좌상단 기준). null 이면 미배치. */
    val posX: Float? = null,
    val posY: Float? = null,
    /** 겹쳐서 묶인 그룹 식별자. null 이면 단독. (그룹 내 최소 id 로 안정화) */
    val groupId: String? = null,
) {
    val isCompleted: Boolean get() = completedAt != null
}
