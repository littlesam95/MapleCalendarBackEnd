package com.sixclassguys.maplecalendar.infrastructure.external.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class DojangRanking(


    @JsonProperty("date")
    val date: String?,

    @JsonProperty("character_class")
    val characterClass: String?,

    @JsonProperty("world_name")
    val worldName: String?,

    @JsonProperty("dojang_best_floor")
    val dojangBestFloor: String?,

    @JsonProperty("date_dojang_record")
    val dateDojangRecord: String?,

    @JsonProperty("dojang_best_time")
    val dojangBestTime: String?,
)