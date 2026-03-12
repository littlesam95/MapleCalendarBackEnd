package com.sixclassguys.maplecalendar.global.exception

class SelfInvitationException(
    override val message: String = "자신의 캐릭터는 초대할 수 없어요."
) : RuntimeException(message)