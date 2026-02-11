package com.sixclassguys.maplecalendar.domain.boss.dto

import com.sixclassguys.maplecalendar.domain.boss.enums.BoardLikeType

data class BossPartyBoardLikeRequest(
    val boardLikeType: BoardLikeType
)