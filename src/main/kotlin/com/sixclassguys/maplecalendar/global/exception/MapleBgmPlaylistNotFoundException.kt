package com.sixclassguys.maplecalendar.global.exception

class MapleBgmPlaylistNotFoundException(
    override val message: String = "플레이리스트를 찾을 수 없어요."
) : RuntimeException(message)