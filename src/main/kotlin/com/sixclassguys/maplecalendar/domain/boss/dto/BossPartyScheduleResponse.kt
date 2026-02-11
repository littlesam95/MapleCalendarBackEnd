package com.sixclassguys.maplecalendar.domain.boss.dto

import com.sixclassguys.maplecalendar.domain.boss.enums.BossDifficulty
import com.sixclassguys.maplecalendar.domain.boss.enums.BossType
import java.time.LocalTime

data class BossPartyScheduleResponse(
    val bossPartyId: Long,
    val boss: BossType,
    val bossDifficulty: BossDifficulty,
    val members: List<BossPartyMemberDetail>,
    val time: String
)