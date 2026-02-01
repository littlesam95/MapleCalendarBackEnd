package com.sixclassguys.maplecalendar.global.exception

import com.sixclassguys.maplecalendar.global.response.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(InvalidApiKeyException::class)
    fun handleInvalidApiKeyException(e: InvalidApiKeyException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            message = e.message
        )

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            message = e.message
        )

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }

    @ExceptionHandler(DeleteBossPartyChatMessageDeniedException::class)
    fun handleDeleteBossPartyChatMessageDeniedException(e: DeleteBossPartyChatMessageDeniedException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            message = e.message
        )

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }

    @ExceptionHandler(MemberNotFoundException::class)
    fun handleMemberNotFound(e: MemberNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(404, e.message))
    }

    @ExceptionHandler(BossPartyNotFoundException::class)
    fun handleBossPartyNotFound(e: BossPartyNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(404, e.message))
    }

    @ExceptionHandler(MapleCharacterNotFoundException::class)
    fun handleMapleCharacterNotFound(e: MapleCharacterNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(404, e.message))
    }

    @ExceptionHandler(BossPartyChatMessageNotFoundException::class)
    fun handleBossPartyChatMessageNotFound(e: BossPartyChatMessageNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(404, e.message))
    }
}