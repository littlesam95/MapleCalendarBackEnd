package com.sixclassguys.maplecalendar.global.exception

class InvalidGoogleIdTokenException(
    override val message: String = "잘못된 구글 계정이에요."
) : RuntimeException(message)