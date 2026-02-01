package com.sixclassguys.maplecalendar.domain.boss.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.sixclassguys.maplecalendar.domain.boss.dto.toResponse
import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyChatMessage
import com.sixclassguys.maplecalendar.domain.boss.service.BossPartyService
import com.sixclassguys.maplecalendar.domain.character.repository.MapleCharacterRepository
import com.sixclassguys.maplecalendar.domain.util.BossPartyChatMessageType
import com.sixclassguys.maplecalendar.global.exception.BossPartyNotFoundException
import com.sixclassguys.maplecalendar.global.exception.MapleCharacterNotFoundException
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Component
class BossPartyChatWebSocketHandler(
    private val bossPartyService: BossPartyService,
    private val mapleCharacterRepository: MapleCharacterRepository,
    private val objectMapper: ObjectMapper // JSON 파싱용
) : TextWebSocketHandler() {

    // key: partyId, value: 해당 파티에 접속 중인 세션 리스트
    private val roomSessions = ConcurrentHashMap<Long, MutableList<WebSocketSession>>()

    // 1. 연결 성립 시
    override fun afterConnectionEstablished(session: WebSocketSession) {
        val partyId = getPartyId(session)
        val characterId = getCharacterId(session)

        roomSessions.computeIfAbsent(partyId) { CopyOnWriteArrayList() }.add(session)

        // DB에 ENTER 타입으로 저장 (필요 시)
        val character = mapleCharacterRepository.findById(characterId).get()
        val systemMsg = bossPartyService.saveMessage(
            partyId, characterId, "${character.characterName}님이 입장하셨습니다.",
            BossPartyChatMessageType.ENTER
        )

        broadcast(partyId, systemMsg)
    }

    // 2. 메시지 수신 시
    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val partyId = getPartyId(session)
        val characterId = getCharacterId(session) // 인증 정보에서 추출

        // 클라이언트로부터 온 JSON 메시지 파싱 (content만 들어있다고 가정)
        val payload = objectMapper.readValue(message.payload, Map::class.java)
        val content = payload["content"] as String

        // DB 저장 (Service 호출)
        val savedMsg = bossPartyService.saveMessage(partyId, characterId, content, BossPartyChatMessageType.TEXT)

        // DTO 변환 후 같은 방 인원들에게 전송
        val response = savedMsg.toResponse(currentCharacterId = -1) // 브로드캐스트용 (isMine은 클라에서 판단)
        val jsonResponse = objectMapper.writeValueAsString(response)

        roomSessions[partyId]?.forEach { s ->
            if (s.isOpen) s.sendMessage(TextMessage(jsonResponse))
        }
    }

    // 3. 연결 종료 시
    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val partyId = getPartyId(session)
        val characterId = getCharacterId(session)

        roomSessions[partyId]?.remove(session)

        val character = mapleCharacterRepository.findById(characterId).get()
        val leaveMsg = bossPartyService.saveMessage(
            partyId,
            characterId,
            "${character.characterName}님이 퇴장하셨습니다.",
            BossPartyChatMessageType.LEAVE
        )

        broadcast(partyId, leaveMsg)
    }

    // 유틸리티: URL 쿼리 파라미터에서 partyId 추출 (예: /ws-chat?partyId=1)
    private fun getPartyId(session: WebSocketSession): Long {
        return session.uri?.query?.split("&")
            ?.find { it.startsWith("partyId=") }
            ?.split("=")?.get(1)?.toLong() ?: throw BossPartyNotFoundException()
    }

    private fun getCharacterId(session: WebSocketSession): Long {
        // 실제로는 Interceptor에서 넣어준 인증 정보를 가져옵니다.
        return session.attributes["characterId"] as? Long ?: throw MapleCharacterNotFoundException()
    }

    private fun broadcast(partyId: Long, message: BossPartyChatMessage) {
        val jsonResponse = objectMapper.writeValueAsString(message)
        roomSessions[partyId]?.forEach { session ->
            synchronized(session) { // 세션별 동기화로 꼬임 방지
                if (session.isOpen) {
                    try {
                        session.sendMessage(TextMessage(jsonResponse))
                    } catch (e: Exception) {
                        println("전송 에러: ${e.message}")
                    }
                }
            }
        }
    }

    fun broadcastDelete(partyId: Long, messageId: Long) {
        // 삭제되었다는 정보를 담은 간단한 JSON 생성
        val deleteSignal = mapOf(
            "type" to "DELETE",
            "messageId" to messageId,
            "isDeleted" to true
        )
        val jsonResponse = objectMapper.writeValueAsString(deleteSignal)

        roomSessions[partyId]?.filter { it.isOpen }?.forEach { s ->
            s.sendMessage(TextMessage(jsonResponse))
        }
    }
}