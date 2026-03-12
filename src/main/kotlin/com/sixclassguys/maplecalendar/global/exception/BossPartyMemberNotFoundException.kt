package com.sixclassguys.maplecalendar.global.exception

class BossPartyMemberNotFoundException(
    override val message: String = "해당 파티의 멤버가 아니에요."
) : RuntimeException(message)