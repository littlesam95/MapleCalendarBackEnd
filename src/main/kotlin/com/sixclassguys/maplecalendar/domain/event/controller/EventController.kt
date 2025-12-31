package com.sixclassguys.maplecalendar.domain.event.controller

import com.sixclassguys.maplecalendar.domain.event.entity.Event
import com.sixclassguys.maplecalendar.domain.event.service.EventService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/events")
class EventController(private val eventService: EventService) {

    @GetMapping
    fun getEvents(
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<List<Event>> {
        val events = eventService.getEventsByMonth(year, month)

        return ResponseEntity.ok(events)
    }
}