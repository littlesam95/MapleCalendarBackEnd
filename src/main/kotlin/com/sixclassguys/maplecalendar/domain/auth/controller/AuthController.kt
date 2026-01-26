package com.sixclassguys.maplecalendar.domain.auth.controller

import com.sixclassguys.maplecalendar.domain.auth.dto.AuthGoogleRequest
import com.sixclassguys.maplecalendar.domain.auth.dto.AuthResult
import com.sixclassguys.maplecalendar.domain.auth.dto.LoginResponse
import com.sixclassguys.maplecalendar.domain.auth.service.AuthService
import com.sixclassguys.maplecalendar.domain.member.entity.Member
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

//    @GetMapping("/characters")
//    suspend fun getCharacters(
//        @RequestHeader("x-nxopen-api-key") apiKey: String
//    ): ResponseEntity<LoginResponse> {
//        // 서비스의 suspend 함수를 직접 호출
//        val response = authService.loginAndGetCharacters(apiKey)
//
//        return ResponseEntity.ok(response)
//    }
//
//    @PostMapping("/auto-login")
//    fun autoLogin(
//        @RequestHeader("x-nxopen-api-key") apiKey: String,
//        @RequestBody request: TokenRequest
//    ): ResponseEntity<AutoLoginResponse> {
//        val result = authService.processAutoLogin(apiKey, request)
//
//        return if (result.isSuccess) {
//            ResponseEntity.ok(result)
//        } else {
//            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result)
//        }
//    }

    data class AppleLoginRequest(val sub: String, val email: String)

    @PostMapping("/google")
    fun googleLogin(
        @RequestBody request: AuthGoogleRequest,
        response: HttpServletResponse // Cookie를 넣기 위해
    ): ResponseEntity<LoginResponse> {

        // 로그인 처리 → AccessToken + RefreshToken 반환
        val (loginResponse, refreshToken) = authService.loginWithGoogle(
            request.idToken,
            request.fcmToken,
            request.platform
        )

        // RefreshToken을 HttpOnly Cookie로 세팅
        val cookie = ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(true) // HTTPS 환경이면 true
            .path("/")
            .maxAge(7 * 24 * 60 * 60) // 7일
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())

        return ResponseEntity.ok(loginResponse)
    }


    @PostMapping("/apple")
    fun appleLogin(@RequestBody request: AppleLoginRequest): ResponseEntity<AuthResult> {
        val member = authService.loginWithApple(request.sub, request.email)
        return ResponseEntity.ok(member)
    }
}