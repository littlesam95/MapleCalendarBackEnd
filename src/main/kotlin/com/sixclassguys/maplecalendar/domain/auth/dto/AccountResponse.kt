package com.sixclassguys.maplecalendar.domain.auth.dto

data class AccountResponse(
    val accountId: String,
    val characters: Map<String, List<AccountCharacterResponse>>
)