package com.sixclassguys.maplecalendar.global.exception

class BossPartyNotFoundException(
    override val message: String = "존재하지 않는 보스 파티에요."
) : RuntimeException(message)