package com.sixclassguys.maplecalendar.domain.boss.dto

import com.sixclassguys.maplecalendar.domain.boss.enums.JoinStatus
import com.sixclassguys.maplecalendar.domain.boss.enums.PartyRole
import com.sixclassguys.maplecalendar.domain.character.entity.MapleCharacter
import java.time.LocalDateTime

data class BossPartyMemberResponse(
    val id: Long,
    val character: MapleCharacter,
    val role: PartyRole,
    val joinStatus: JoinStatus,
    val joinedAt: LocalDateTime
)

