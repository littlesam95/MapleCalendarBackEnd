package com.sixclassguys.maplecalendar.global.exception

class InvalidBossPartyChatReportException(
    override val message: String = "신고할 수 없는 메시지에요."
) : RuntimeException(message)