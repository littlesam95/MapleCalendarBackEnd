package com.sixclassguys.maplecalendar.global.exception

class InvalidBossPartyTransferLeaderException(
    override val message: String = "파티장을 양도할 수 없어요."
) : RuntimeException(message)