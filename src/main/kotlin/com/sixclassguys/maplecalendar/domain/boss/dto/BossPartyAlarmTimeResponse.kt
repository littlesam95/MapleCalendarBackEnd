package com.sixclassguys.maplecalendar.domain.boss.dto

import com.sixclassguys.maplecalendar.domain.boss.enums.RegistrationMode
import java.time.LocalDateTime

data class BossPartyAlarmTimeResponse(
    val id: Long,
    val alarmTime: LocalDateTime,
    val message: String,
    val isSent: Boolean,
    val registrationMode: RegistrationMode,
)