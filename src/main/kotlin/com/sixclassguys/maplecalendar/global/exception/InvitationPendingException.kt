package com.sixclassguys.maplecalendar.global.exception

class InvitationPendingException(
    override val message: String = "이미 가입 대기 중인 유저에요."
) : RuntimeException(message)