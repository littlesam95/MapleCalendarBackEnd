package com.sixclassguys.maplecalendar.global.exception

class AlreadyPartyMemberException(
    override val message: String = "이미 파티에 가입된 유저에요."
) : RuntimeException(message)