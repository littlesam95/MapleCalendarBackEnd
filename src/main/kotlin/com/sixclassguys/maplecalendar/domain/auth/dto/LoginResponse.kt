package com.sixclassguys.maplecalendar.domain.auth.dto

data class LoginResponse(
    val representativeOcid: String? = null, // 이미 설정되어 있다면 ocid 반환
    val characters: Map<String, List<AccountCharacterResponse>> = emptyMap()
)