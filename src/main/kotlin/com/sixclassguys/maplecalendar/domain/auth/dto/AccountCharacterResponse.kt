package com.sixclassguys.maplecalendar.domain.auth.dto

data class AccountCharacterResponse(
    val ocid: String,
    val characterName: String,
    val worldName: String,
    val characterClass: String,
    val characterLevel: Int
)