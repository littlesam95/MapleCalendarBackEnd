package com.sixclassguys.maplecalendar.global.exception

class BossPartyBoardUnauthorizedException(
    override val message: String = "파티 멤버만 게시글을 조회할 수 있어요."
) : RuntimeException(message)