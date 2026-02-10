package com.sixclassguys.maplecalendar.domain.boss.controller

import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyAlarmPeriodRequest
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyAlarmTimeRequest
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyAlarmTimeResponse
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyChatMessageResponse
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyCreateRequest
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyCreateResponse
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyDetailResponse
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyMemberResponse
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyResponse
import com.sixclassguys.maplecalendar.domain.boss.handler.BossPartyChatWebSocketHandler
import com.sixclassguys.maplecalendar.domain.boss.service.BossPartyService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Slice
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Boss Party API", description = "보스 파티 관련 정보 조회 API")
@RestController
@RequestMapping("/api/boss-parties")
class BossPartyController(
    private val bossPartyService: BossPartyService,
    private val webSocketHandler: BossPartyChatWebSocketHandler
) {

    @PostMapping
    fun createBossParty(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody @Valid req: BossPartyCreateRequest
    ): ResponseEntity<BossPartyCreateResponse> {
        val partyId = bossPartyService.createParty(req, userDetails.username)
        return ResponseEntity.ok(BossPartyCreateResponse(partyId))
    }

    // 나중에 jwt에 memberId 넣는식으로 고쳐서 쓰는게 편할듯
    @Operation(summary = "회원이 속한 보스 파티 조회", description = "로그인한 회원의 이메일을 기준으로 가입된 모든 보스 파티 목록을 조회합니다.")
    @GetMapping
    fun getBossParties(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<List<BossPartyResponse>> {
        val bossParties = bossPartyService.getBossParties(userDetails.username)

        return ResponseEntity.ok(bossParties)
    }

    @GetMapping("/{bossPartyId}")
    fun getBossPartyDetail(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable bossPartyId: Long
    ): ResponseEntity<BossPartyDetailResponse> {
        val bossPartyDetail = bossPartyService.getBossPartyDetail(bossPartyId, userDetails.username)

        return ResponseEntity.ok(bossPartyDetail)
    }

    @Operation(
        summary = "보스 파티 알람 시간 조회",
        description = "보스 파티 ID를 기반으로 해당 파티에 설정된 알람 시간 목록을 조회합니다."
    )
    @GetMapping("/{bossPartyId}/alarm-times")
    fun getBossPartyAlarmTimes(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Parameter(description = "조회할 보스 파티의 ID", required = true)
        @PathVariable bossPartyId: Long
    ): ResponseEntity<List<BossPartyAlarmTimeResponse>> {
        val response = bossPartyService.getBossPartyAlarmTimes(bossPartyId)

        return ResponseEntity.ok(response)
    }

    @PatchMapping("/{bossPartyId}/alarm-times/toggle")
    fun updateAlarmSetting(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable bossPartyId: Long,
        @RequestParam enabled: Boolean // 쿼리 스트링 (?enabled=true)
    ): ResponseEntity<Unit> {
        bossPartyService.togglePartyAlarm(userDetails.username, bossPartyId, enabled)

        return ResponseEntity.ok().build()
    }

    @PostMapping("/{bossPartyId}/alarm-times")
    fun createAlarm(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable bossPartyId: Long,
        @RequestBody request: BossPartyAlarmTimeRequest
    ): ResponseEntity<List<BossPartyAlarmTimeResponse>> {
        bossPartyService.createAlarmTime(
            partyId = bossPartyId,
            userEmail = userDetails.username,
            hour = request.hour,
            minute = request.minute,
            date = request.date,
            message = request.message
        )
        val response = bossPartyService.getBossPartyAlarmTimes(bossPartyId)

        return ResponseEntity.ok(response)
    }

    @PatchMapping("/{bossPartyId}/alarm-period")
    fun updateAlarmPeriod(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable bossPartyId: Long,
        @RequestBody request: BossPartyAlarmPeriodRequest
    ): ResponseEntity<List<BossPartyAlarmTimeResponse>> {
        bossPartyService.updateBossPartyAlarmPeriod(
            partyId = bossPartyId,
            userEmail = userDetails.username,
            request = request
        )
        val response = bossPartyService.getBossPartyAlarmTimes(bossPartyId)

        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{bossPartyId}/alarm-times/{alarmId}")
    fun deleteAlarm(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable bossPartyId: Long,
        @PathVariable alarmId: Long
    ): ResponseEntity<List<BossPartyAlarmTimeResponse>> {
        bossPartyService.deleteAlarm(bossPartyId, alarmId, userDetails.username)
        val response = bossPartyService.getBossPartyAlarmTimes(bossPartyId)

        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "보스 파티 수락된 멤버 조회",
        description = "보스 파티 ID를 기반으로 해당 파티에 참여를 수락한 멤버 목록을 조회합니다."
    )
    @GetMapping("/{bossPartyId}/members/accepted")
    fun getAcceptedMembers(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Parameter(description = "조회할 보스 파티의 ID", required = true)
        @PathVariable bossPartyId: Long
    ): List<BossPartyMemberResponse> {
        return bossPartyService.getAcceptedMembersByBossPartyId(bossPartyId)
    }

    @Operation(
        summary = "보스 파티 채팅 메시지 조회",
        description = "보스 파티 ID를 기반으로 해당 파티에 등록된 채팅 메시지 목록을 조회합니다."
    )
    @GetMapping("/{bossPartyId}/chat-messages")
    fun getChatMessages(
        @Parameter(description = "조회할 보스 파티의 ID", required = true)
        @PathVariable bossPartyId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Slice<BossPartyChatMessageResponse>>{
        val response = bossPartyService.getChatMessages(bossPartyId, userDetails.username, page, size)

        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{bossPartyId}/chat-messages/{messageId}")
    fun deleteMessage(
        @AuthenticationPrincipal userDetails: UserDetails, // Spring Security 인증 정보
        @PathVariable bossPartyId: Long,
        @PathVariable messageId: Long
    ): ResponseEntity<Unit> {
        // 1. DB 상태 변경 (isDeleted = true)
        val deletedMessage = bossPartyService.deleteMessage(bossPartyId, messageId, userDetails.username)

        // 2. WebSocket으로 모든 파티원에게 "메시지 상태 변경" 알림 전송
        webSocketHandler.broadcastDelete(deletedMessage.bossParty.id, messageId)

        return ResponseEntity.ok().build()
    }
}