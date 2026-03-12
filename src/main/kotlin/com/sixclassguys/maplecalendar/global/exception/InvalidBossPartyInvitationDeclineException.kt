package com.sixclassguys.maplecalendar.global.exception

class InvalidBossPartyInvitationDeclineException(
    override val message: String = "보스 파티 초대를 거절할 수 없어요."
) : RuntimeException(message)