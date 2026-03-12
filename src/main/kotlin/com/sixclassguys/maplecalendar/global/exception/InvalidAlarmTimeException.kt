package com.sixclassguys.maplecalendar.global.exception

class InvalidAlarmTimeException(
    override val message: String = "알람 예약 시간이 잘못되었어요."
) : RuntimeException(message)