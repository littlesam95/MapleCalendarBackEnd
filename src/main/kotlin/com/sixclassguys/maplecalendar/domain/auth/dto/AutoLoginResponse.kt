package com.sixclassguys.maplecalendar.domain.auth.dto

import com.sixclassguys.maplecalendar.infrastructure.external.dto.CharacterBasic

data class AutoLoginResponse(
    val isSuccess: Boolean,
    val message: String? = null,
    val characterBasic: CharacterBasic? = null
)