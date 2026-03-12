package com.sixclassguys.maplecalendar.global.exception

class InvalidBossPartyLeaderException(
    override val message: String = "파티장만 해당 기능을 이용할 수 있어요."
) : RuntimeException(message)