package com.sixclassguys.maplecalendar.domain.notification.controller

import com.sixclassguys.maplecalendar.domain.auth.dto.TokenRequest
import com.sixclassguys.maplecalendar.domain.notification.dto.FcmTokenRequest
import com.sixclassguys.maplecalendar.domain.notification.service.NotificationService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    @PostMapping("/tokens")
    fun registerToken(@RequestBody request: FcmTokenRequest): ResponseEntity<Unit> {
        notificationService.registerToken(request)

        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/tokens")
    fun unregisterToken(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody request: TokenRequest,
    ): ResponseEntity<Unit> {
        notificationService.unregisterToken(userDetails.username, request.refreshToken)

        return ResponseEntity.ok().build()
    }
}