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
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyScheduleResponse
import com.sixclassguys.maplecalendar.domain.boss.handler.BossPartyChatWebSocketHandler
import com.sixclassguys.maplecalendar.domain.boss.service.BossPartyService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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

    @GetMapping("/schedules")
    fun getBossPartySchedules(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam day: Int
    ): ResponseEntity<List<BossPartyScheduleResponse>> {
        val response = bossPartyService.getDailyBossSchedules(year, month, day, userDetails.username)

        return ResponseEntity.ok(response)
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
        @PathVariable bossPartyId: Long
    ): ResponseEntity<Boolean> {
        val response = bossPartyService.togglePartyAlarm(userDetails.username, bossPartyId)

        return ResponseEntity.ok(response)
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

    @PatchMapping("/{bossPartyId}/chat-messages/toggle")
    fun updateChatAlarmSetting(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable bossPartyId: Long
    ): ResponseEntity<Boolean> {
        val response = bossPartyService.togglePartyChatAlarm(userDetails.username, bossPartyId)

        return ResponseEntity.ok(response)
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

    @PatchMapping("/{bossPartyId}/chat-messages/{messageId}")
    fun hideMessage(
        @AuthenticationPrincipal userDetails: UserDetails, // Spring Security 인증 정보
        @PathVariable bossPartyId: Long,
        @PathVariable messageId: Long
    ): ResponseEntity<Unit> {
        // 1. DB 상태 변경 (isDeleted = true)
        val hiddenMessage = bossPartyService.hideChatMessage(bossPartyId, messageId, userDetails.username)

        // 2. WebSocket으로 모든 파티원에게 "메시지 상태 변경" 알림 전송
        webSocketHandler.broadcastHide(hiddenMessage.bossParty.id, hiddenMessage)

        return ResponseEntity.ok().build()
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

    @Operation(summary = "파티 멤버 초대", description = "특정 캐릭터를 파티에 초대합니다. 리더만 호출 가능")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "초대 성공"),
        ApiResponse(responseCode = "403", description = "권한 없음"),
        ApiResponse(responseCode = "400", description = "잘못된 요청")
    )
    @PostMapping("/{bossPartyId}/invite")
    fun inviteMember(
        @AuthenticationPrincipal
        @Parameter(description = "인증된 사용자 이메일", required = true)
        userDetails: UserDetails,

        @PathVariable
        @Parameter(description = "초대할 파티 ID", required = true)
        bossPartyId: Long,

        @RequestParam
        @Parameter(description = "초대할 캐릭터 ID", required = true)
        characterId: Long
    ): ResponseEntity<Unit> {
        bossPartyService.inviteMember(
            partyId = bossPartyId,
            inviteeId = characterId,
            userEmail = userDetails.username
        )
        return ResponseEntity.ok().build()
    }

    @Operation(summary = "파티 초대 수락", description = "초대받은 파티를 수락합니다")
    @PostMapping("/{bossPartyId}/accept")
    fun acceptInvitation(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable bossPartyId: Long,
        @RequestParam characterId: Long
    ): ResponseEntity<Unit> {
        bossPartyService.acceptInvitation(
            partyId = bossPartyId,
            characterId = characterId,
            userEmail = userDetails.username
        )
        return ResponseEntity.ok().build()
    }

    @Operation(summary = "파티 초대 거절", description = "초대받은 파티를 거절합니다")
    @DeleteMapping("/{bossPartyId}/decline")
    fun declineInvitation(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable bossPartyId: Long,
        @RequestParam characterId: Long
    ): ResponseEntity<Unit> {
        bossPartyService.declineInvitation(
            partyId = bossPartyId,
            characterId = characterId,
            userEmail = userDetails.username
        )
        return ResponseEntity.ok().build()
    }

    @Operation(summary = "파티 멤버 추방", description = "리더가 멤버를 파티에서 추방합니다")
    @DeleteMapping("/{bossPartyId}/members/{characterId}")
    fun kickMember(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable bossPartyId: Long,
        @PathVariable characterId: Long
    ): ResponseEntity<Unit> {
        bossPartyService.kickMember(
            partyId = bossPartyId,
            characterId = characterId,
            userEmail = userDetails.username
        )
        return ResponseEntity.ok().build()
    }

    @Operation(summary = "파티 탈퇴", description = "본인이 파티에서 탈퇴합니다")
    @DeleteMapping("/{bossPartyId}/leave")
    fun leaveParty(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable bossPartyId: Long,
        @RequestParam characterId: Long
    ): ResponseEntity<Unit> {
        bossPartyService.leaveParty(
            partyId = bossPartyId,
            characterId = characterId,
            userEmail = userDetails.username
        )
        return ResponseEntity.ok().build()
    }

    @Operation(summary = "파티장 양도", description = "리더가 다른 멤버에게 파티장 권한을 양도합니다")
    @PatchMapping("/{bossPartyId}/transfer")
    fun transferLeader(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable bossPartyId: Long,
        @RequestParam targetCharacterId: Long
    ): ResponseEntity<Unit> {
        bossPartyService.transferLeader(
            partyId = bossPartyId,
            targetCharacterId = targetCharacterId,
            userEmail = userDetails.username
        )
        return ResponseEntity.ok().build()
    }
}