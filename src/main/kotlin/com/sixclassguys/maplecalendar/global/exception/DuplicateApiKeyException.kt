package com.sixclassguys.maplecalendar.global.exception

class DuplicateApiKeyException(
    override val message: String = "이미 다른 사용자가 등록한 Nexon API Key에요."
) : RuntimeException(message)