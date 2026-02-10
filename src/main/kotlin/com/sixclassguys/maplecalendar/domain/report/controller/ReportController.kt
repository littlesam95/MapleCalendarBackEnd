package com.sixclassguys.maplecalendar.domain.report.controller

import com.sixclassguys.maplecalendar.domain.report.dto.ChatReportRequest
import com.sixclassguys.maplecalendar.domain.report.service.ReportService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val chatReportService: ReportService
) {

    @PostMapping("/chat")
    fun reportChat(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody request: ChatReportRequest
    ): ResponseEntity<Unit> {
        val userEmail = userDetails.username

        chatReportService.reportChat(
            userEmail = userEmail,
            chatId = request.chatId,
            reason = request.reason,
            detail = request.reasonDetail
        )

        return ResponseEntity.ok().build()
    }
}