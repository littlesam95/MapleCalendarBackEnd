package com.sixclassguys.maplecalendar.domain.eventalarm.dto

data class AlarmRequest(
    val eventId: Long,
    val isEnabled: Boolean,
    val alarmTimes: List<String>
)