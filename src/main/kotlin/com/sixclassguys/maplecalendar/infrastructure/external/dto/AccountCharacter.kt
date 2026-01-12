package com.sixclassguys.maplecalendar.infrastructure.external.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class AccountCharacter(
    @JsonProperty("ocid")
    val ocid: String,

    @JsonProperty("character_name")
    val characterName: String,

    @JsonProperty("world_name")
    val worldName: String,

    @JsonProperty("character_class")
    val characterClass: String,

    @JsonProperty("character_level")
    val characterLevel: Int
)