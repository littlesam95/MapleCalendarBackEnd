package com.sixclassguys.maplecalendar.domain.auth.controller

import com.sixclassguys.maplecalendar.domain.auth.dto.AutoLoginResponse
import com.sixclassguys.maplecalendar.domain.auth.dto.LoginResponse
import com.sixclassguys.maplecalendar.domain.auth.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @GetMapping("/characters")
    suspend fun getCharacters(
        @RequestHeader("x-nxopen-api-key") apiKey: String
    ): ResponseEntity<LoginResponse> {
        // 서비스의 suspend 함수를 직접 호출
        val response = authService.loginAndGetCharacters(apiKey)

        return ResponseEntity.ok(response)
    }

    @PostMapping("/auto-login")
    fun autoLogin(
        @RequestHeader("x-nxopen-api-key") apiKey: String
    ): ResponseEntity<AutoLoginResponse> {
        val result = authService.processAutoLogin(apiKey)

        return if (result.isSuccess) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result)
        }
    }
}