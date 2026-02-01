package com.sixclassguys.maplecalendar.global.exception

class BossPartyChatMessageNotFoundException(
    override val message: String = "삭제하려는 메시지가 존재하지 않습니다."
) : RuntimeException(message)