package com.sixclassguys.maplecalendar.global.exception

class DuplicateEmailException(
    override val message: String = "이미 등록된 이메일이에요."
) : RuntimeException(message)