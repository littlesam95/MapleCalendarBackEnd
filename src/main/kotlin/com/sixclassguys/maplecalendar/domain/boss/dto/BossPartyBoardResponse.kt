package com.sixclassguys.maplecalendar.domain.boss.dto

import java.time.LocalDateTime

data class BossPartyBoardResponse(
    val id: Long,
    val memberId: Long,
    val characterName: String,
    val content: String,
    val createdAt: LocalDateTime,
    val images: List<String>,   // 이미지 URL 목록
    val likesCount: Int         // 좋아요 수
)
