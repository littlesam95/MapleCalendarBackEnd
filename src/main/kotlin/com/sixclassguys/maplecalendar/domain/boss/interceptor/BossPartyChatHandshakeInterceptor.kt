package com.sixclassguys.maplecalendar.domain.boss.interceptor

import com.sixclassguys.maplecalendar.domain.auth.jwt.JwtUtil
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyMemberRepository
import com.sixclassguys.maplecalendar.global.exception.BossPartyNotFoundException
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

@Component
class BossPartyChatHandshakeInterceptor(
    private val jwtUtil: JwtUtil,
    private val bossPartyMemberRepository: BossPartyMemberRepository
) : HandshakeInterceptor {

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        val servletRequest = (request as ServletServerHttpRequest).servletRequest
        val token = servletRequest.getParameter("token") // ws://.../ws-chat?token=ACCESS_TOKEN
        val partyIdStr = servletRequest.getParameter("partyId")

        return try {
            if (token != null && partyIdStr != null) {
                val claims = jwtUtil.parseClaims(token)
                val username = claims.subject
                val partyId = partyIdStr.toLong()

                // 보스 파티에 '승인된' 멤버 정보를 가져옴
                val partyMember = bossPartyMemberRepository.findByBossPartyIdAndCharacterMemberEmail(partyId, username)
                    ?: throw BossPartyNotFoundException("해당 파티에 참여 중인 캐릭터를 찾을 수 없습니다.")

                // Handler에서 사용할 수 있도록 세션 속성에 저장
                attributes["characterId"] = partyMember.character.id
                attributes["username"] = username
                attributes["partyId"] = partyId

                true // 인증 및 경로 검증 성공
            } else {
                false
            }
        } catch (e: Exception) {
            println("WebSocket 인증 실패: ${e.message}")
            false
        }
    }

    override fun afterHandshake(request: ServerHttpRequest, response: ServerHttpResponse, wsHandler: WebSocketHandler, exception: Exception?) {}
}