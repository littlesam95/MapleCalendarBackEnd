package com.sixclassguys.maplecalendar.global.exception

class DeleteBossPartyChatMessageDeniedException(
    override val message: String = "본인이 작성한 메시지만 삭제할 수 있습니다."
) : RuntimeException(message)