package com.sixclassguys.maplecalendar.domain.boss.dto

import com.sixclassguys.maplecalendar.domain.boss.enums.PartyRole

data class BossPartyMemberDetail(
    val characterId: Long,
    val characterName: String,
    val worldName: String,
    val characterClass: String,
    val characterLevel: Long,
    val characterImage: String,
    val role: PartyRole,
    val isMyCharacter: Boolean,
    val joinedAt: String
)