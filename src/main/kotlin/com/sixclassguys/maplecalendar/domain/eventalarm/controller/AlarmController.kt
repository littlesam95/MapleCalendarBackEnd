package com.sixclassguys.maplecalendar.domain.eventalarm.controller

import com.sixclassguys.maplecalendar.domain.event.dto.EventResponse
import com.sixclassguys.maplecalendar.domain.eventalarm.dto.AlarmRequest
import com.sixclassguys.maplecalendar.domain.eventalarm.service.AlarmService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/alarms")
class AlarmController(
    private val alarmService: AlarmService
) {

    @PostMapping("/event")
    fun saveEventAlarm(
        @RequestHeader("x-nxopen-api-key") apiKey: String,
        @RequestBody request: AlarmRequest
    ): ResponseEntity<EventResponse> {
        val updatedEvent = alarmService.saveOrUpdateAlarm(apiKey, request)

        return ResponseEntity.ok(updatedEvent)
    }

    // 💡 알람 스위치 토글 전용 API
    @PatchMapping("/toggle/{eventId}")
    fun toggleAlarm(
        @RequestHeader("x-nxopen-api-key") apiKey: String,
        @PathVariable eventId: Long
    ): ResponseEntity<EventResponse> {
        val updatedEvent = alarmService.toggleAlarmStatus(apiKey, eventId)
        return ResponseEntity.ok(updatedEvent)
    }
}