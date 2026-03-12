package com.sixclassguys.maplecalendar.global.exception

class RepresentativeCharacterUnauthorizedException(
    override val message: String = "본인의 캐릭터만 대표로 설정할 수 있어요."
) : RuntimeException(message)