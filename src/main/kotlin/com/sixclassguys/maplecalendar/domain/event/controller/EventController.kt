package com.sixclassguys.maplecalendar.domain.event.controller

import com.sixclassguys.maplecalendar.domain.event.dto.EventResponse
import com.sixclassguys.maplecalendar.domain.event.service.EventService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable eventId: Long
    ): ResponseEntity<EventResponse> {
        val event = eventService.getEventDetail(userDetails.username, eventId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(event)
    }

    @GetMapping
    fun getEvents(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<List<EventResponse>> {
        val events = eventService.getEventsByMonth(year, month, userDetails.username)

        return ResponseEntity.ok(events)
    }

    @GetMapping("/today")
    fun getTodayEvents(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam day: Int
    ): ResponseEntity<List<EventResponse>> {
        val events = eventService.getTodayEvents(year, month, day, userDetails.username)

        return ResponseEntity.ok(events)
    }

        @GetMapping("/daily")
    fun getDailyEvents(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam day: Int
    ): ResponseEntity<List<EventResponse>> {
        val events = eventService.getTodayEvents(year, month, day, "")

        return ResponseEntity.ok(events)
    }
}