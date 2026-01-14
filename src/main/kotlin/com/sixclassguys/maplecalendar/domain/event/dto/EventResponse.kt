package com.sixclassguys.maplecalendar.domain.event.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class EventResponse(
    val id: Long,
    val title: String,
    val url: String,

    @JsonProperty("thumbnail_url")
    val thumbnailUrl: String?,

    val startDate: String,
    val endDate: String,
    val isRegistered: Boolean = false, // 이 이벤트에 알람을 설정했는가?
    val alarmTimes: List<String> = emptyList() // 설정된 구체적인 시간들
)