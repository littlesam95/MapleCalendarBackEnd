package com.sixclassguys.maplecalendar.global.exception

class MemberNotFoundException(
    override val message: String = "해당 유저를 찾을 수 없어요."
) : RuntimeException(message)