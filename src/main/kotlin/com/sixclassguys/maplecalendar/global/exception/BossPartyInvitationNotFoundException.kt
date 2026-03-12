package com.sixclassguys.maplecalendar.global.exception

class BossPartyInvitationNotFoundException(
    override val message: String = "보스 파티에 초대받은 기록이 존재하지 않아요."
) : RuntimeException(message)