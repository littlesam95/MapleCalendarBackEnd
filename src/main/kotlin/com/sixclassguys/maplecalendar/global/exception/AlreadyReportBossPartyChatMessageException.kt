package com.sixclassguys.maplecalendar.global.exception

class AlreadyReportBossPartyChatMessageException(
    override val message: String = "이미 신고한 메시지에요."
) : RuntimeException(message)