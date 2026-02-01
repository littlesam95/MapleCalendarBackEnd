package com.sixclassguys.maplecalendar.domain.boss.dto

import java.time.LocalDateTime

data class BossPartyResponse(
    val id: Long,
    val title: String,
    val description: String,
    val boss: String,
    val difficulty: String,
    val createdAt: LocalDateTime?
)

