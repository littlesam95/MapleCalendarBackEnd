package com.sixclassguys.maplecalendar.global.dto

data class RedisAlarmDto(
    val type: AlarmType,     // EVENT 또는 BOSS
    val targetId: Long,      // EventAlarmTime ID 또는 BossPartyAlarmTime ID (발송 후 DB 업데이트용)
    val memberId: Long,      // 수신자 ID
    val title: String,       // 알림 제목
    val message: String      // 알림 내용
)

enum class AlarmType { EVENT, BOSS }