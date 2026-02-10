package com.sixclassguys.maplecalendar.domain.boss.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyChatMessageResponse
import com.sixclassguys.maplecalendar.domain.boss.dto.toResponse
import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyChatMessage
import com.sixclassguys.maplecalendar.domain.boss.service.BossPartyService
import com.sixclassguys.maplecalendar.domain.character.repository.MapleCharacterRepository
import com.sixclassguys.maplecalendar.domain.boss.enums.BossPartyChatMessageType
import com.sixclassguys.maplecalendar.domain.notification.service.NotificationService
import com.sixclassguys.maplecalendar.global.exception.BossPartyNotFoundException
import com.sixclassguys.maplecalendar.global.exception.MapleCharacterNotFoundException
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Component
class BossPartyChatWebSocketHandler(
    private val bossPartyService: BossPartyService,
    private val notificationService: NotificationService,
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
        activeMembers.computeIfAbsent(partyId) { ConcurrentHashMap.newKeySet() }.add(characterId)

        // DB에 ENTER 타입으로 저장 (필요 시)
        val character = mapleCharacterRepository.findById(characterId).get()
        val systemMsg = bossPartyService.saveMessage(
            partyId, characterId, "${character.characterName}님이 입장하셨습니다.",
            BossPartyChatMessageType.ENTER
        )
        val message = systemMsg.toResponse(characterId)

        broadcast(partyId, message)
    }

    // 2. 메시지 수신 시
    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val partyId = getPartyId(session)
        // Interceptor가 attributes에 넣어준 ID 활용 (클라이언트가 보낸 것보다 보안상 안전)
        val characterId = session.attributes["characterId"] as Long
        val username = session.attributes["username"] as String

        val payload = objectMapper.readValue(message.payload, Map::class.java)
        val content = payload["content"] as String

        // DB 저장 및 브로드캐스트
        val savedMsg = bossPartyService.saveMessage(partyId, characterId, content, BossPartyChatMessageType.TEXT)
        val message = savedMsg.toResponse(characterId)

        broadcast(partyId, message)

        // 2. 비동기로 채팅 알림 발송
        // savedMsg에서 보낸 사람 이름 등을 가져와 전달
        CompletableFuture.runAsync {
            notificationService.sendBossPartyChatAlarm(
                partyId = partyId,
                senderCharacterId = characterId,
                content = content,
                senderName = savedMsg.character.characterName
            )
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
        val message = leaveMsg.toResponse(characterId)

        broadcast(partyId, message)
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

    private fun broadcast(partyId: Long, message: BossPartyChatMessageResponse) {
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

    fun broadcastHide(partyId: Long, updatedChat: BossPartyChatMessage) {
        val hideResponse = updatedChat.toResponse(0L)
        val jsonResponse = objectMapper.writeValueAsString(hideResponse)

        roomSessions[partyId]?.forEach { session ->
            synchronized(session) {
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
        val deleteResponse = BossPartyChatMessageResponse(
            id = messageId,
            messageType = BossPartyChatMessageType.DELETED,
            isDeleted = true,
            senderId = 0L,
            senderName = "",
            senderWorld = "",
            senderImage = "",
            content = "",
            isMine = false,
            isHidden = false,
            createdAt = "",
        )
        val jsonResponse = objectMapper.writeValueAsString(deleteResponse)
        roomSessions[partyId]?.filter { it.isOpen }?.forEach { s -> s.sendMessage(TextMessage(jsonResponse)) }
    }

    companion object {

        // 현재 어느 파티에 어떤 캐릭터가 접속 중인지 추적하기 위한 Map
        // Key: partyId, Value: Set<characterId>
        private val activeMembers = ConcurrentHashMap<Long, MutableSet<Long>>()

        fun getActiveCharacterIds(partyId: Long): Set<Long> {
            return activeMembers[partyId] ?: emptySet()
        }
    }
}