package com.sixclassguys.maplecalendar.global.exception

class MapleBgmNotFoundInPlaylistException(
    override val message: String = "플레이리스트에 존재하지 않는 BGM이에요."
) : RuntimeException(message)