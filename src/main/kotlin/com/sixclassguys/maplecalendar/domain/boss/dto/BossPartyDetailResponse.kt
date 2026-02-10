package com.sixclassguys.maplecalendar.domain.boss.dto

import com.sixclassguys.maplecalendar.domain.boss.enums.BossDifficulty
import com.sixclassguys.maplecalendar.domain.boss.enums.BossType
import java.time.DayOfWeek
import java.time.LocalDateTime

data class BossPartyDetailResponse(
    val id: Long,
    val title: String,
    val description: String,
    val boss: BossType,
    val difficulty: BossDifficulty,
    val alarms: List<BossPartyAlarmTimeResponse>,
    val members: List<BossPartyMemberDetail>,
    val isLeader: Boolean,
    val isPartyAlarmEnabled: Boolean,
    val isChatAlarmEnabled: Boolean,
    val alarmDayOfWeek: DayOfWeek?,
    val alarmHour: Int?,
    val alarmMinute: Int?,
    val alarmMessage: String?,
    val createdAt: LocalDateTime?
)