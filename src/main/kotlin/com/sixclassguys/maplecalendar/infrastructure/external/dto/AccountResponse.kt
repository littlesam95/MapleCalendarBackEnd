package com.sixclassguys.maplecalendar.infrastructure.external.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class AccountResponse(
    @JsonProperty("account_list")
    val accounts: List<Account>
)