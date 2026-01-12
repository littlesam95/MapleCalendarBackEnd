package com.sixclassguys.maplecalendar.global.exception

class MemberNotFoundException(
    override val message: String = "해당 API Key를 가진 유저를 찾을 수 없습니다."
) : RuntimeException(message)