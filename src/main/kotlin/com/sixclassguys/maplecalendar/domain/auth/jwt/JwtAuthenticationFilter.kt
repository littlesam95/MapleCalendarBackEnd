package com.sixclassguys.maplecalendar.domain.auth.jwt

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.security.SignatureException
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

        // 헤더가 없거나 형식이 다르면 다음 필터로 넘김 (인증 안 됨)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val accessToken = authHeader.substring(7)

        try {
            // AccessToken 검증
            val claims = jwtUtil.parseClaims(accessToken)
            val subject = claims.subject

            // AccessToken인 경우에만 인증 처리 (타입 검증 추가 권장)
            if (claims["type"] == "access" && subject != null) {
                setAuthentication(claims.subject)
            }
        } catch (e: ExpiredJwtException) {
            // 만료된 경우 여기서 처리하지 않고 그냥 보냅니다.
            // Spring Security 설정에 의해 401 에러가 발생하게 되고,
            // 클라이언트(앱)는 이를 보고 /reissue를 호출하게 됩니다.
            SecurityContextHolder.clearContext()
            logger.info("토큰이 만료되었습니다: ${e.message}")
        } catch (e: SignatureException) {
            SecurityContextHolder.clearContext()
            logger.error("서명 불일치! 발급 키와 검증 키가 다른지 확인하세요: ${e.message}")
        } catch (e: MalformedJwtException) {
            SecurityContextHolder.clearContext()
            logger.error("토큰 구조가 잘못됨: ${e.message}")
        } catch (e: Exception) {
            logger.error("토큰 검증 중 오류 발생: ${e.message}")
        }

        filterChain.doFilter(request, response)
    }

    private fun setAuthentication(username: String) {
        // 단순 String이 아니라 Spring Security가 제공하는 User 객체(UserDetails의 구현체)를 만듭니다.
        val userDetails: UserDetails = User.builder()
            .username(username)
            .password("") // 비밀번호는 토큰 인증이라 필요 없으므로 빈 값
            .authorities("ROLE_USER")
            .build()

        val auth = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
        SecurityContextHolder.getContext().authentication = auth
    }
}