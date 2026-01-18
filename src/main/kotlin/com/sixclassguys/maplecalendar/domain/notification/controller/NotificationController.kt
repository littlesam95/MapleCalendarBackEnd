package com.sixclassguys.maplecalendar.domain.notification.controller

import com.sixclassguys.maplecalendar.domain.notification.dto.TokenRequest
import com.sixclassguys.maplecalendar.domain.notification.service.NotificationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    @PostMapping("/tokens")
    fun registerToken(@RequestBody request: TokenRequest): ResponseEntity<Unit> {
        notificationService.registerToken(request)

        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/tokens")
    fun unregisterToken(
        @RequestHeader("x-nxopen-api-key") apiKey: String,
        @RequestBody request: TokenRequest,
    ): ResponseEntity<Unit> {
        notificationService.unregisterToken(apiKey, request.token)

        return ResponseEntity.ok().build()
    }
}