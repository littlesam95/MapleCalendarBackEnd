package com.sixclassguys.maplecalendar.global.exception

import com.sixclassguys.maplecalendar.global.response.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    // 400 Bad Request : 잘못된 요청
    @ExceptionHandler(
        InvalidAlarmTimeException::class,
        SelfInvitationException::class,
        InvalidBossPartyInvitationDeclineException::class,
        InvalidBossPartyKickException::class,
        InvalidBossPartyTransferLeaderException::class,
        InvalidBossPartyAcceptInvitationException::class,
        AlreadyReportBossPartyChatMessageException::class
    )
    fun handleBadRequest(e: Exception): ResponseEntity<ErrorResponse> {
        return buildResponse(HttpStatus.BAD_REQUEST, e.message)
    }

    // 401 Unauthorized : 권한 없음
    @ExceptionHandler(
        InvalidApiKeyException::class,
        RepresentativeCharacterUnauthorizedException::class, // Spring MVC 수준에서 발생할 때만 작동
        DeleteBossPartyChatMessageDeniedException::class,
        InvalidJwtRefreshTokenException::class,
        ExpiredJwtRefreshTokenException::class,
        InvalidGoogleIdTokenException::class,
        InvalidAppleIdTokenException::class,
        InvalidBossPartyLeaderException::class,
        BossPartyAlarmUnauthorizedException::class,
        BossPartyBoardUnauthorizedException::class,
        MapleBgmPlaylistUnauthorizedException::class,
        MapleBgmNotFoundInPlaylistException::class,
        InvalidBossPartyChatReportException::class
    )
    fun handleUnauthorized(e: Exception): ResponseEntity<ErrorResponse> {
        return buildResponse(HttpStatus.UNAUTHORIZED, e.message)
    }

    // 404 Not Found : 찾을 수 없음
    @ExceptionHandler(
        MemberNotFoundException::class,
        BossPartyNotFoundException::class,
        MapleCharacterNotFoundException::class,
        BossPartyChatMessageNotFoundException::class,
        MapleBgmNotFoundException::class,
        MapleBgmPlaylistNotFoundException::class,
        EventNotFoundException::class,
        PlatformNotFoundException::class,
        BossPartyMemberNotFoundException::class,
        BossPartyAlarmNotFoundException::class,
        BossPartyInvitationNotFoundException::class,
        BossPartyBoardNotFoundException::class
    )
    fun handleNotFound(e: Exception): ResponseEntity<ErrorResponse> {
        return buildResponse(HttpStatus.NOT_FOUND, e.message)
    }

    // 409 Conflict : 충돌
    @ExceptionHandler(
        DuplicateMapleBgmException::class,
        DuplicateEmailException::class,
        AlreadyPartyMemberException::class,
        InvitationPendingException::class,
        DuplicateApiKeyException::class
    )
    fun handleConflict(e: Exception): ResponseEntity<ErrorResponse> {
        return buildResponse(HttpStatus.CONFLICT, e.message)
    }

    // 서버 내부 에러 (예상치 못한 모든 예외)
    @ExceptionHandler(Exception::class)
    fun handleAll(e: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unexpected Error: ${e.javaClass.simpleName}, Message: ${e.message}")
        log.error("Cause: ${e.cause?.javaClass?.simpleName}")

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했어요.")
    }

    private fun buildResponse(status: HttpStatus, message: String?): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = status.value(),
            message = message ?: "알 수 없는 에러가 발생했어요."
        )
        return ResponseEntity.status(status).body(errorResponse)
    }
}