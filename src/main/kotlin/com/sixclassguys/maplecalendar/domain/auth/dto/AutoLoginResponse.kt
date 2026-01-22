package com.sixclassguys.maplecalendar.domain.auth.dto

import com.sixclassguys.maplecalendar.infrastructure.external.dto.CharacterBasic
import com.sixclassguys.maplecalendar.infrastructure.external.dto.DojangRanking
import com.sixclassguys.maplecalendar.infrastructure.external.dto.Ranking
import com.sixclassguys.maplecalendar.infrastructure.external.dto.UnionResponse

data class AutoLoginResponse(
    val isSuccess: Boolean,
    val message: String? = null,
    val characterBasic: CharacterBasic? = null,
    val isGlobalAlarmEnabled: Boolean = false,

    val characterPopularity: Int? = null,
    val characterOverallRanking: Ranking? = null,
    val characterServerRanking: Ranking? = null,
    val characterUnionLevel: UnionResponse? = null,
    val characterDojang: DojangRanking? = null
)