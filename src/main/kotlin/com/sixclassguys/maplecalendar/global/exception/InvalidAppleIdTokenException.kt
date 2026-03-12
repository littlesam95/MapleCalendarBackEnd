package com.sixclassguys.maplecalendar.global.exception

class InvalidAppleIdTokenException(
    override val message: String = "잘못된 애플 계정이에요."
) : RuntimeException(message)