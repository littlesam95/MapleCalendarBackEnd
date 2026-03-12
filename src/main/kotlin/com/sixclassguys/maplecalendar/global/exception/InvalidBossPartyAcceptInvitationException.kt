package com.sixclassguys.maplecalendar.global.exception

class InvalidBossPartyAcceptInvitationException (
    override val message: String = "보스 파티 초대를 수락할 수 없어요."
) : RuntimeException(message)