package com.sixclassguys.maplecalendar.global.exception

class InvalidJwtRefreshTokenException(
    override val message: String = "인증 정보에 오류가 있어요."
) : RuntimeException(message)