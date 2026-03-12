package com.sixclassguys.maplecalendar.global.exception

class BossPartyAlarmNotFoundException(
    override val message: String = "존재하지 않는 보스 파티 알람이에요."
) : RuntimeException(message)