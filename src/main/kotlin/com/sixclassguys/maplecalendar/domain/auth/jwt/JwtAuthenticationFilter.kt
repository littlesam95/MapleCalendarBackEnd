package com.sixclassguys.maplecalendar.domain.auth.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JwtUtil
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        println("DEBUG: Auth Header = $authHeader") // 1. 헤더가 오는지 확인

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            println("DEBUG: Header Missing or Invalid Format")
            filterChain.doFilter(request, response)
            return
        }

        val accessToken = authHeader.substring(7)
        try {
            val claims = jwtUtil.parseClaims(accessToken)
            println("DEBUG: Claims Subject = ${claims.subject}") // 2. 파싱 성공 여부 확인
            setAuthentication(claims.subject)
        } catch (e: Exception) {
            println("DEBUG: Auth Error = ${e.message}") // 3. 에러 내용 확인
        }
        filterChain.doFilter(request, response)
    }

    private fun setAuthentication(username: String) {
        // 단순 String이 아니라 Spring Security가 제공하는 User 객체(UserDetails의 구현체)를 만듭니다.
        val userDetails: UserDetails = User.builder()
            .username(username)
            .password("") // 비밀번호는 토큰 인증이라 필요 없으므로 빈 값
            .roles("USER") // 👈 이 한 줄이 있어야 '인증된 사용자'로 인정됩니다.
            .build()

        val auth = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
        SecurityContextHolder.getContext().authentication = auth

        println("DEBUG: SecurityContext에 인증 정보 저장 완료 - ${SecurityContextHolder.getContext().authentication?.name}")
    }
}