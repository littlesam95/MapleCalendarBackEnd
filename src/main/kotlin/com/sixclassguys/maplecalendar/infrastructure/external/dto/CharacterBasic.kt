package com.sixclassguys.maplecalendar.infrastructure.external.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class CharacterBasic(
    @JsonProperty("date")
    val date: String? = null,

    @JsonProperty("character_name")
    val characterName: String? = null,

    @JsonProperty("world_name")
    val worldName: String? = null,

    @JsonProperty("character_gender")
    val characterGender: String? = null,

    @JsonProperty("character_class")
    val characterClass: String? = null,

    @JsonProperty("character_class_level")
    val characterClassLevel: String? = null,

    @JsonProperty("character_level")
    val characterLevel: Long? = null,

    @JsonProperty("character_exp")
    val characterExp: Long? = null,

    @JsonProperty("character_exp_rate")
    val characterExpRate: String? = null,

    @JsonProperty("character_guild_name")
    val characterGuildName: String? = null,

    @JsonProperty("character_image")
    val characterImage: String? = "",

    @JsonProperty("character_date_create")
    val characterDateCreate: String? = null,

    @JsonProperty("access_flag")
    val accessFlag: String? = null,

    @JsonProperty("liberation_quest_clear")
    val liberationQuestClear: String? = null
)