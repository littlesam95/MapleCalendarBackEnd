package com.sixclassguys.maplecalendar.domain.event.controller

import com.sixclassguys.maplecalendar.domain.event.dto.EventResponse
import com.sixclassguys.maplecalendar.domain.event.service.EventService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/events")
class EventController(
    private val eventService: EventService
) {

    @GetMapping("/{eventId}")
    fun getEvent(
        @RequestHeader("x-nxopen-api-key") apiKey: String,
        @PathVariable eventId: Long
    ): ResponseEntity<EventResponse> {
        val event = eventService.getEventDetail(apiKey, eventId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(event)
    }

    @GetMapping
    fun getEvents(
        @RequestHeader("x-nxopen-api-key") apiKey: String,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<List<EventResponse>> {
        val events = eventService.getEventsByMonth(year, month, apiKey)

        return ResponseEntity.ok(events)
    }

    @GetMapping("/today")
    fun getTodayEvents(
        @RequestHeader("x-nxopen-api-key") apiKey: String,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam day: Int
    ): ResponseEntity<List<EventResponse>> {
        val events = eventService.getTodayEvents(year, month, day, apiKey)

        return ResponseEntity.ok(events)
    }
}