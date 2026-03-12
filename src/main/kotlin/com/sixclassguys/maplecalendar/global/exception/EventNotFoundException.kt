package com.sixclassguys.maplecalendar.global.exception

class EventNotFoundException(
    override val message: String = "이벤트를 찾을 수 없어요."
) : RuntimeException(message)