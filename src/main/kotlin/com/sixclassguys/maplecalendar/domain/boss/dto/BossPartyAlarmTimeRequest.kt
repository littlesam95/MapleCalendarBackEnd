package com.sixclassguys.maplecalendar.domain.boss.dto

import java.time.LocalDate

data class BossPartyAlarmTimeRequest(
    val hour: Int,
    val minute: Int,
    val date: LocalDate,
    val message: String
)