package com.sixclassguys.maplecalendar.domain.boss.dto

import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyChatMessage

data class BossPartySystemEvent(
    val partyId: Long,
    val characterId: Long,
    val message: BossPartyChatMessage
)