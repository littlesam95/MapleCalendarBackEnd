package com.sixclassguys.maplecalendar.infrastructure.external.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class Account(
    @JsonProperty("account_id")
    val accountId: String,

    @JsonProperty("character_list")
    val characters: List<AccountCharacter>
)