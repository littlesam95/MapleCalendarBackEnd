package com.sixclassguys.maplecalendar.global.exception

class MapleBgmPlaylistUnauthorizedException(
    override val message: String = "내 플레이리스트이거나, 공개된 플레이리스트가 아니에요."
) : RuntimeException(message)