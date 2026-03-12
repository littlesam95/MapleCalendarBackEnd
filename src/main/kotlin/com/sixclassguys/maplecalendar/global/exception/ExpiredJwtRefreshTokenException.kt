package com.sixclassguys.maplecalendar.global.exception

class ExpiredJwtRefreshTokenException(
    override val message: String = "인증 정보가 만료되었어요. 다시 로그인해주세요."
) : RuntimeException(message)