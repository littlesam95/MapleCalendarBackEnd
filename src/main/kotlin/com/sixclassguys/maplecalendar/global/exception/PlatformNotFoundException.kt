package com.sixclassguys.maplecalendar.global.exception

class PlatformNotFoundException(
    override val message: String = "안드로이드나 iOS를 사용하는 기기가 아니에요."
) : RuntimeException(message)