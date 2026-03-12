package com.sixclassguys.maplecalendar.global.exception

class BossPartyCapacityExceededException(
    override val message: String = "이미 해당 보스 파티에 최대 인원이 가입되었어요."
) : RuntimeException(message)