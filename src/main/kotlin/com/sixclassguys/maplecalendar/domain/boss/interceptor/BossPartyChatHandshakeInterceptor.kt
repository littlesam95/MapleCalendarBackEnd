package com.sixclassguys.maplecalendar.domain.boss.interceptor

import com.sixclassguys.maplecalendar.domain.auth.jwt.JwtUtil
import com.sixclassguys.maplecalendar.domain.character.repository.MapleCharacterRepository
import com.sixclassguys.maplecalendar.domain.member.repository.MemberRepository
import com.sixclassguys.maplecalendar.global.exception.MapleCharacterNotFoundException
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

@Component
class BossPartyChatHandshakeInterceptor(
    private val jwtUtil: JwtUtil,
    private val memberRepository: MemberRepository, // 멤버를 통해 캐릭터 ID를 찾기 위함
    private val mapleCharacterRepository: MapleCharacterRepository
) : HandshakeInterceptor {

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        val servletRequest = (request as ServletServerHttpRequest).servletRequest
        val token = servletRequest.getParameter("token") // ws://.../ws-chat?token=ACCESS_TOKEN

        return try {
            if (token != null) {
                val claims = jwtUtil.parseClaims(token)
                val username = claims.subject // 토큰의 주체 (보통 email이나 아이디)

                // 1. 유저 정보 조회
                // 💡 채팅 메시지 저장 시 characterId가 필요하므로 여기서 찾아서 세션에 넣어줍니다.
                // 대표 캐릭터를 가져오거나, 쿼리 파라미터로 받은 캐릭터 ID를 검증하는 로직이 필요할 수 있습니다.
                val character = mapleCharacterRepository.findFirstByMemberEmailAndIsActiveTrue(username)
                    ?: throw MapleCharacterNotFoundException("활성화된 캐릭터를 찾을 수 없습니다.")

                // 2. WebSocketSession의 attributes에 저장
                // 이후 Handler에서 session.attributes["characterId"]로 꺼내 쓸 수 있습니다.
                attributes["characterId"] = character.id
                attributes["username"] = username

                true // 연결 허용
            } else {
                false
            }
        } catch (e: Exception) {
            println("WebSocket 인증 실패: ${e.message}")
            false // 연결 거부
        }
    }

    override fun afterHandshake(request: ServerHttpRequest, response: ServerHttpResponse, wsHandler: WebSocketHandler, exception: Exception?) {}
}