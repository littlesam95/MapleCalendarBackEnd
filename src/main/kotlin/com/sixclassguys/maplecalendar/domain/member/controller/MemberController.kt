package com.sixclassguys.maplecalendar.domain.member.controller

import com.sixclassguys.maplecalendar.domain.member.dto.RepresentativeOcidRequest
import com.sixclassguys.maplecalendar.domain.member.service.MemberService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/member")
class MemberController(
    private val memberService: MemberService
) {

    @PatchMapping("/representative")
    fun setRepresentative(
        @RequestHeader("x-nxopen-api-key") apiKey: String,
        @RequestBody request: RepresentativeOcidRequest
    ): ResponseEntity<Unit> {
        memberService.updateRepresentativeCharacter(apiKey, request.ocid)

        return ResponseEntity.noContent().build() // 성공 시 204 No Content 반환
    }

    @PatchMapping("/alarm-status")
    fun updateAlarmStatus(
        @RequestHeader("x-nxopen-api-key") apiKey: String
    ): ResponseEntity<Boolean> {
        val isGlobalAlarmEnabled = memberService.updateGlobalAlarmStatus(apiKey)

        return ResponseEntity.ok(isGlobalAlarmEnabled)
    }
}