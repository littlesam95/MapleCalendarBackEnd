package com.sixclassguys.maplecalendar.domain.boss.dto

import java.time.DayOfWeek

data class BossPartyAlarmPeriodRequest(
    val dayOfWeek: DayOfWeek?,
    val hour: Int,
    val minute: Int,
    val message: String,
    val isImmediateApply: Boolean
)