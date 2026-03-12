package com.sixclassguys.maplecalendar.global.exception

class InvalidApiKeyException(
    override val message: String = "유효하지 않은 넥슨 Open API Key에요."
) : RuntimeException(message)