package com.sixclassguys.maplecalendar.global.exception

class MapleCharacterNotFoundException(
    override val message: String = "존재하지 않는 캐릭터입니다."
) : RuntimeException(message)