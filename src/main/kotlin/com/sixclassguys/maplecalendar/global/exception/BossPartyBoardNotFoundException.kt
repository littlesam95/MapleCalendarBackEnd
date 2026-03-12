package com.sixclassguys.maplecalendar.global.exception

class BossPartyBoardNotFoundException(
    override val message: String = "게시글을 찾을 수 없어요."
) : RuntimeException(message)