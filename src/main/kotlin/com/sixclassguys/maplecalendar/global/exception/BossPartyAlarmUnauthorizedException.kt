package com.sixclassguys.maplecalendar.global.exception

class BossPartyAlarmUnauthorizedException(
    override val message: String = "해당 보스 파티의 알람이 아니에요."
) : RuntimeException(message)